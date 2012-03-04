package lila.system
package ai

import lila.chess.{ Game, Move }
import model.DbGame

final class StupidAi extends Ai {

  def apply(dbGame: DbGame): Valid[(Game, Move)] = {

    val game = dbGame.toChess

    for {
      destination ← game.situation.destinations.headOption toValid "Game is finished"
      (orig, dests) = destination
      dest ← dests.headOption toValid "No moves from " + orig
      newChessGameAndMove ← game(orig, dest)
    } yield newChessGameAndMove
  }
}
