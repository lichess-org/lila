package lila.ai

import chess.format.UciMove
import chess.Move

import lila.analyse.AnalysisMaker
import lila.game.{ Game, Progress, GameRepo, PgnRepo }

trait Ai {

  def play(game: Game, level: Int): Fu[Progress] = withValidSituation(game) {
    for {
      fen ← game.variant.exotic ?? { GameRepo initialFen game.id }
      pgn ← PgnRepo get game.id
      moveStr ← move(pgn, fen, level)
      result ← (for {
        uciMove ← UciMove(moveStr) toValid "Wrong bestmove: " + moveStr
        result ← (game.toChess withPgnMoves pgn)(uciMove.orig, uciMove.dest)
      } yield result).future
      (c, m) = result
      (progress, pgn2) = game.update(c, m)
      _ ← (GameRepo save progress) >> PgnRepo.save(game.id, pgn2)
    } yield progress
  }

  def move(pgn: String, initialFen: Option[String], level: Int): Fu[String]

  def analyse(pgn: String, initialFen: Option[String]): Fu[AnalysisMaker]

  private def withValidSituation[A](game: Game)(op: ⇒ Fu[A]): Fu[A] =
    if (game.toChess.situation playable true) op
    else fufail("[ai stockfish] invalid game situation: " + game.toChess.situation)
}
