package lila
package ai

import chess.{ Game, Move }
import model.DbGame

import scalaz.effects._

final class StupidAi extends Ai {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = io {

    val game = dbGame.toChess

    for {
      destination ← game.situation.destinations.headOption toValid "Game is finished"
      (orig, dests) = destination
      dest ← dests.headOption toValid "No moves from " + orig
      newChessGameAndMove ← game(orig, dest)
    } yield newChessGameAndMove
  }
}
