package lila.ai
package stockfish

import chess.Rook
import chess.format.UciDump
import chess.format.Forsyth
import lila.analyse.Analysis
import model.{ GetQueueSize, QueueSize }
import model.play.BestMove

import scala.concurrent.duration._
import akka.actor.{ Props, Actor, ActorRef, Kill }
import akka.pattern.{ ask, AskTimeoutException }
import play.api.Play.current
import play.api.libs.concurrent.Akka.system

final class Server(execPath: String, config: Config) {

  def play(pgn: String, initialFen: Option[String], level: Int): Fu[Valid[String]] = {
    implicit val timeout = makeTimeout(playAtMost)
    UciDump(pgn, initialFen) map { moves ⇒
      model.play.Task.Builder(moves, initialFen map chess960Fen, level)
    } fold (
      err ⇒ fuccess(failure(err)),
      play ⇒ {
        (actor ? play).mapTo[BestMove] map (m ⇒ success(~m.move))
      } ~ { _ onFailure reboot }
    )
  }

  def analyse(pgn: String, initialFen: Option[String]): Fu[Valid[String ⇒ Analysis]] =
    UciDump(pgn, initialFen).fold(
      err ⇒ fuccess(failure(err)),
      moves ⇒ {
        val analyse = model.analyse.Task.Builder(moves, initialFen map chess960Fen)
        implicit val timeout = makeTimeout(analyseAtMost)
        (actor ? analyse).mapTo[Valid[String ⇒ Analysis]] ~ { _ onFailure reboot }
      }
    )

  private def chess960Fen(fen: String) = (Forsyth << fen).fold(fen) { situation ⇒
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

  private val playAtMost = 10 seconds
  private val analyseAtMost = 20 minutes

  private lazy val process = Process(execPath, "StockFish") _
  private lazy val actor = system.actorOf(Props(
    new ActorFSM(process, config)))
}
