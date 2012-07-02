package lila
package ai.stockfish

import model.play.BestMove
import game.DbGame

trait Stockfish {

  protected def applyMove(dbGame: DbGame, move: String) = for {
    bestMove ← BestMove(move.some filter ("" !=)).parse toValid "Wrong bestmove " + move
    result ← dbGame.toChess(bestMove.orig, bestMove.dest)
  } yield result
}
