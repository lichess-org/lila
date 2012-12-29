package lila
package setup

import chess.{ Game, Board, Variant, Clock, Speed }
import game.{ GameRepo, DbGame, Pov }

trait Config {

  // Whether or not to use a clock
  val clock: Boolean

  // Clock time in minutes
  val time: Int

  // Clock increment in seconds
  val increment: Int

  // Game variant code
  val variant: Variant

  // Creator player color
  val color: Color

  lazy val creatorColor = color.resolve

  def makeGame = Game(
    board = Board init variant,
    clock = makeClock)

  def validClock = clock.fold(time + increment > 0, true)

  def makeClock = clock option Clock(time * 60, increment)
}

trait GameGenerator { self: Config ⇒

  def game: DbGame

  def pov = Pov(game, creatorColor)
}

object Config extends BaseConfig

trait BaseConfig {

  val variants = Variant.all map (_.id)
  val variantChoices = Variant.all map { v ⇒ v.id.toString -> v.name }
  val variantDefault = Variant.Standard

  val speeds = Speed.all map (_.id)

  val timeMin = 0
  val timeMax = 30
  val times = (timeMin to timeMax).toList

  val incrementMin = 0
  val incrementMax = 30
  val increments = (incrementMin to incrementMax).toList
}
