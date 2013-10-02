package lila.ai
package stockfish

import scala.concurrent.duration._

import akka.actor.{ Props, Actor, ActorRef, Kill }
import akka.pattern.{ ask, AskTimeoutException }
import chess.format.Forsyth
import chess.format.UciDump
import chess.Variant
import play.api.libs.concurrent.Akka.system
import play.api.Play.current

import actorApi._
import lila.analyse.AnalysisMaker
import lila.hub.actorApi.ai.GetLoad

private[ai] final class Server(
    queue: ActorRef,
    config: Config,
    val uciMemo: lila.game.UciMemo) extends lila.ai.Ai {

  def move(uciMoves: String, initialFen: Option[String], level: Int): Fu[String] = {
    implicit val timeout = makeTimeout(config.playTimeout)
    queue ? PlayReq(uciMoves, initialFen map chess960Fen, level) mapTo
      manifest[Valid[String]] flatMap (_.future)
  }

  def analyse(uciMoves: String, initialFen: Option[String]): Fu[AnalysisMaker] = {
    implicit val timeout = makeTimeout(config.analyseTimeout)
    queue ? FullAnalReq(uciMoves, initialFen map chess960Fen) mapTo
      manifest[Valid[AnalysisMaker]] flatMap (_.future)
  }

  def load: Fu[Int] = {
    import makeTimeout.short
    queue ? GetLoad mapTo manifest[Int] addEffect { l ⇒
      (l > 0) ! println(s"[stockfish] load = $l")
    }
  }

  private def chess960Fen(fen: String) = (Forsyth << fen).fold(fen) { situation ⇒
    fen.replace("KQkq", situation.board.pieces.toList filter {
      case (_, piece) ⇒ piece is chess.Rook
    } sortBy {
      case (pos, _) ⇒ (pos.y, pos.x)
    } map {
      case (pos, piece) ⇒ piece.color.fold(pos.file.toUpperCase, pos.file)
    } mkString "")
  }
}
