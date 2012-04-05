package lila

import chess._
import model._

import scalaz.effects._

trait Ai {

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]]
}
