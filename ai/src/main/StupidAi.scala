package lila.ai

import chess.{ Game, Move }

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

private[ai] final class StupidAi extends Ai {

  def play(game: Game, pgn: String, initialFen: Option[String], level: Int): Future[Valid[(Game, Move)]] = Future {
    for {
      destination ← game.situation.destinations.headOption toValid "Game is finished"
      (orig, dests) = destination
      dest ← dests.headOption toValid "No moves from " + orig
      newChessGameAndMove ← game(orig, dest)
    } yield newChessGameAndMove
  }

  def analyse(pgn: String, initialFen: Option[String]) = 
    throw new RuntimeException("Stupid analysis is not implemented")
}
