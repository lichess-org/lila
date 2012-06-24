package lila
package ai

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._

trait Ai {

  def apply(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]]
}
