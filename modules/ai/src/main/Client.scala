package lila.ai

import actorApi._

import akka.actor._
import akka.pattern.ask

import lila.analyse.Info
import lila.common.ws.WS
import lila.game.{ Game, GameRepo }
import chess.format.UciMove

final class Client(
    config: Config,
    endpoint: String,
    val uciMemo: lila.game.UciMemo) {

  private def withValidSituation[A](game: Game)(op: => Fu[A]): Fu[A] =
    if (game.toChess.situation playable true) op
    else fufail("[ai stockfish] invalid game situation: " + game.toChess.situation)

  def play(game: Game, level: Int): Fu[PlayResult] = withValidSituation(game) {
    for {
      fen ← game.variant.exotic ?? { GameRepo initialFen game.id }
      uciMoves ← uciMemo.get(game)
      moveResult ← move(uciMoves.toList, fen, level)
      uciMove ← (UciMove(moveResult.move) toValid s"${game.id} wrong bestmove: $moveResult").future
      result ← game.toChess(uciMove.orig, uciMove.dest, uciMove.promotion).future
      (c, move) = result
      progress = game.update(c, move)
      _ ← (GameRepo save progress) >>- uciMemo.add(game, uciMove.uci)
    } yield PlayResult(progress, move)
  }

  def move(uciMoves: List[String], initialFen: Option[String], level: Int): Fu[MoveResult] = {
    implicit val timeout = makeTimeout(config.playTimeout)
    WS.url(s"$endpoint/play").withQueryString(
      "uciMoves" -> uciMoves.mkString(" "),
      "initialFen" -> ~initialFen,
      "level" -> level.toString
    ).get() map (_.body) map MoveResult.apply
  }

  def analyse(uciMoves: List[String], initialFen: Option[String]): Fu[List[Info]] = {
    implicit val timeout = makeTimeout(config.analyseTimeout)
    WS.url(s"$endpoint/analyse").withQueryString(
      "uciMoves" -> uciMoves.mkString(" "),
      "initialFen" -> ~initialFen
    ).get() map (_.body) map Info.decodeList flatten "Can't read analysis results: "
  }
}
