package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor.{ Props, Actor, ActorRef, Kill }
import akka.pattern.{ ask, AskTimeoutException }
import model.{ GetQueueSize, QueueSize }
import play.api.libs.concurrent.Akka.system
import play.api.Play.current

import actorApi._
import chess.format.Forsyth
import chess.format.UciDump
import chess.Rook
import lila.analyse.Analysis

final class Server(config: Config) {

  def play(pgn: String, initialFen: Option[String], level: Int): Fu[String] = {
    implicit val timeout = makeTimeout(config.playTimeout)
    UciDump(pgn, initialFen) fold (
      err ⇒ fufail(err),
      moves ⇒ actor ? Req(moves, initialFen map chess960Fen, level, false) mapTo manifest[String] 
    )
  }

  def analyse(pgn: String, initialFen: Option[String]): Fu[String ⇒ Analysis] =
    fufail("not implemented")
  // UciDump(pgn, initialFen).fold(
  //   err ⇒ fufail(err),
  //   moves ⇒ {
  //     val analyse = model.analyse.Task.Builder(moves, initialFen map chess960Fen)
  //     implicit val timeout = makeTimeout(config.analyseTimeout)
  //     (actor ? analyse).mapTo[String ⇒ Analysis] ~ { _ onFailure reboot }
  //   }
  // )

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

  private lazy val actor = system.actorOf(Props(new Queue(config)))
}
