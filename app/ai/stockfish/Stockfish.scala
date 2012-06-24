package lila
package ai.stockfish

import model.BestMove
import game.DbGame

trait Stockfish {

  protected def applyMove(dbGame: DbGame, move: String) = for {
    bestMove ← BestMove(move.some filter ("" !=)).parse toValid "Wrong bestmove " + move
    (orig, dest) = bestMove
    result ← dbGame.toChess(orig, dest)
  } yield result
}
