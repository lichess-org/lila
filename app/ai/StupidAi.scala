package lila
package ai

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._
import scala.concurrent.Future

final class StupidAi extends Ai with common.Futuristic {

  def play(dbGame: DbGame, pgn: String, initialFen: Option[String]): Future[Valid[(Game, Move)]] = Future {

    val game = dbGame.toChess

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
