package lila
package setup

import chess.{ Variant, Standard, Mode }
import elo.EloRange

case class AiConfig(variant: Variant, level: Int, color: Color) extends Config {

  def >> = (variant.id, level, color.name).some
}

object AiConfig extends BaseConfig {

  def <<(v: Int, level: Int, c: String) = new AiConfig(
    variant = Variant(v) err "Invalid game variant " + v,
    level = level,
    color = Color(c) err "Invalid color " + c)

  val default = AiConfig(
    variant = variantDefault, 
    level = 1, 
    color = Color.default)

  val levelChoices = (1 to 8).toList map { l ⇒ l.toString -> l.toString }
}

//case class HookConfig(eloRange: Option[String]) 
//extends HumanConfig with EloRange 
