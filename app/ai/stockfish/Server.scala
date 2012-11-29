package lila
package ai
package stockfish

import chess.Rook
import chess.format.UciDump
import chess.format.Forsyth
import analyse.Analysis
import model.{ GetQueueSize, QueueSize }
import model.play.BestMove

import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import akka.actor.{ Props, Actor, ActorRef, Kill }
import akka.pattern.{ ask, AskTimeoutException }
import play.api.Play.current
import play.api.libs.concurrent._
import scalaz.effects._

final class Server(
    execPath: String,
    config: Config) {

  def play(pgn: String, initialFen: Option[String], level: Int): Future[Valid[String]] = {
    implicit val timeout = new Timeout(playAtMost)
    (for {
      moves ← UciDump(pgn, initialFen)
      play = model.play.Task.Builder(moves, initialFen map chess960Fen, level)
    } yield play).fold(
      err ⇒ Future(failure(err)),
      play ⇒ {
        (actor ? play).mapTo[BestMove] map ((m: BestMove) ⇒ success(~m.move))
      } ~ { _ onFailure reboot }
    )
  }

  def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]] =
    UciDump(pgn, initialFen).fold(
      err ⇒ Future(failure(err)),
      moves ⇒ {
        val analyse = model.analyse.Task.Builder(moves, initialFen map chess960Fen)
        implicit val timeout = Timeout(analyseAtMost)
        (actor ? analyse).mapTo[Valid[Analysis]] ~ { _ onFailure reboot }
      }
    )

  def report: Future[Int] = {
    implicit val timeout = new Timeout(playAtMost)
    actor ? GetQueueSize map {
      case QueueSize(s) ⇒ s
    }
  }

  private def chess960Fen(fen: String) = (Forsyth << fen).fold(fen) { situation =>
    fen.replace("KQkq", situation.board.pieces.toList filter {
      case (_, piece) ⇒ piece is Rook
    } sortBy {
      case (pos, _) ⇒ (pos.y, pos.x)
    } map {
      case (pos, piece) ⇒ piece.color.fold(pos.file.toUpperCase, pos.file)
    } mkString "")
  }

  private val reboot: PartialFunction[Throwable, Unit] = {
    case e: AskTimeoutException ⇒ actor ! model.RebootException
  }

  private implicit val executor = Akka.system.dispatcher

  private val playAtMost = 20 seconds
  private val analyseAtMost = 20 minutes

  private lazy val process = Process(execPath, "StockFish") _
  private lazy val actor = Akka.system.actorOf(Props(
   new ActorFSM(process, config)))
}
