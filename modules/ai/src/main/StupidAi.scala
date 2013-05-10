package lila.ai

import chess.{ Game, Move }

private[ai] final class StupidAi extends Ai {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Fu[(Game, Move)] = (for {
    destination ← game.situation.destinations.headOption toValid "Game is finished"
    (orig, dests) = destination
    dest ← dests.headOption toValid "No moves from " + orig
    newChessGameAndMove ← game(orig, dest)
  } yield newChessGameAndMove).future

  def analyse(pgn: String, initialFen: Option[String]) =
    throw new RuntimeException("Stupid analysis is not implemented")
}
