package lila.ai
package stockfish

import akka.actor._
import akka.pattern.ask
import chess.format.UciMove
import remote.actorApi._

import lila.analyse.Info
import lila.hub.actorApi.ai.GetLoad

final class Client(
    dispatcher: ActorRef,
    fallback: lila.ai.Ai,
    config: Config,
    val uciMemo: lila.game.UciMemo) extends lila.ai.Ai {

  def move(uciMoves: List[String], initialFen: Option[String], level: Int): Fu[MoveResult] = {
    implicit val timeout = makeTimeout(config.playTimeout)
    dispatcher ? Play(uciMoves, ~initialFen, level) mapTo manifest[MoveResult]
  } recoverWith {
    case e: Exception => {
      logwarn(s"[stockfish client] move ${e.getMessage}")
      fallback.move(uciMoves, initialFen, level)
    }
  }

  def analyse(uciMoves: List[String], initialFen: Option[String]): Fu[List[Info]] = {
    implicit val timeout = makeTimeout(config.analyseTimeout)
    dispatcher ? Analyse(uciMoves, ~initialFen) mapTo manifest[String] map
      Info.decodeList flatten "Can't read analysis results: "
  } recoverWith {
    case e: Exception => {
      logwarn(s"[stockfish client] analyse ${e.getMessage}")
      fallback.analyse(uciMoves, initialFen)
    }
  }

  def load: Fu[List[Option[Int]]] = {
    implicit val timeout = makeTimeout(config.loadTimeout)
    dispatcher ? GetLoad mapTo manifest[List[Option[Int]]]
  }
}
