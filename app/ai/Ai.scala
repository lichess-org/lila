package lila
package ai

import chess.{ Game, Move }
import game.DbGame
import analyse.Analysis

import scalaz.effects._

trait Ai {

  def play(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]]

  def analyse(dbGame: DbGame, initialFen: Option[String]): IO[Valid[Analysis]]
}
