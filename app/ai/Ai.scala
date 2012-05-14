package lila
package ai

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._

trait Ai {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]]
}
