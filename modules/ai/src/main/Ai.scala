package lila.ai

import chess.format.{ UciMove, UciDump }
import chess.Move

import lila.analyse.Info
import lila.game.{ Game, Progress, GameRepo, UciMemo }

case class AiHost(host: String, ip: String)
case class MoveResult(move: String, server: AiHost)
case class PlayResult(progress: Progress, move: Move, server: AiHost)

trait Ai {

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
    } yield PlayResult(progress, move, moveResult.server)
  }

  def uciMemo: UciMemo

  def move(uciMoves: List[String], initialFen: Option[String], level: Int): Fu[MoveResult]

  def analyse(uciMoves: List[String], initialFen: Option[String]): Fu[List[Info]]

  private def withValidSituation[A](game: Game)(op: ⇒ Fu[A]): Fu[A] =
    if (game.toChess.situation playable true) op
    else fufail("[ai stockfish] invalid game situation: " + game.toChess.situation)
}
