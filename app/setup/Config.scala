package lila
package setup

import chess.{ Variant, Mode }
import elo.EloRange
import game.{ GameRepo, DbGame, Pov }

trait Config {

  // Game variant code
  val variant: Variant

  // Creator player color
  val color: Color

  lazy val creatorColor = color.resolve

  def game: DbGame

  def pov = Pov(game, creatorColor)
}

trait HumanConfig extends Config with EloRange {

  // Whether or not to use a clock
  val clock: Boolean

  // Clock time in minutes
  val time: Option[Int]

  // Clock increment in seconds
  val increment: Option[Int]

  // casual or rated
  val mode: Mode
}

object Config extends BaseConfig

trait BaseConfig {

  val variants = Variant.all map (_.id)
  val variantChoices = Variant.all map { v ⇒ v.id.toString -> v.name }
  val variantDefault = Variant.Standard
}

//case class HookConfig(eloRange: Option[String]) 
//extends HumanConfig with EloRange 
