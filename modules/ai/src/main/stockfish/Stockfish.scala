package lila.ai
package stockfish

import actorApi.BestMove

import chess.Game
import chess.format.UciMove

object Stockfish {

  def applyMove(game: Game, pgn: String, move: String): Fu[(Game, chess.Move)] = (for {
    bestMove ← UciMove(~BestMove(move.some filter ("" !=)).move) toValid "Wrong bestmove " + move
    result ← (game withPgnMoves pgn)(bestMove.orig, bestMove.dest)
  } yield result).future
}
