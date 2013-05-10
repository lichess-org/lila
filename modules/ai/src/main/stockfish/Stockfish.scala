package lila.ai
package stockfish

import model.play.BestMove
import chess.Game

object Stockfish {

  def applyMove(game: Game, pgn: String, move: String): Fu[(Game, chess.Move)] = (for {
    bestMove ← model.play.BestMove(move.some filter ("" !=)).parse toValid "Wrong bestmove " + move
    result ← (game withPgnMoves pgn)(bestMove.orig, bestMove.dest)
  } yield result).future
}
