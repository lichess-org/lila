package lila
package setup

import chess.{ Variant }
import game.{ GameRepo, DbGame, Pov }

trait Config {

  // Game variant code
  val variant: Variant

  // Creator player color
  val color: Color

  lazy val creatorColor = color.resolve
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
}
