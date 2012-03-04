package lila.system

import lila.chess._
import model._

trait Ai {

  def apply(dbGame: DbGame): Valid[(Game, Move)]
}
