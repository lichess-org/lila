package lila
package ai.stockfish

import model.play.BestMove
import game.DbGame

trait Stockfish {

  protected def applyMove(dbGame: DbGame, pgn: String, move: String) = for {
    bestMove ← BestMove(move.some filter ("" !=)).parse toValid "Wrong bestmove " + move
    chessGame = dbGame.toChess withPgnMoves pgn
    result ← chessGame(bestMove.orig, bestMove.dest)
  } yield result
}
