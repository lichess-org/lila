package lila
package setup

import elo.EloRange
import chess.Mode

import play.api.data.Forms._

object Mappings {

  val variant = number.verifying(Config.variants contains _)
  val clock = boolean
  val time = number.verifying(HookConfig.times contains _)
  val increment = number.verifying(HookConfig.increments contains _)
  def mode(isAuth: Boolean) = optional(number
    .verifying(HookConfig.modes contains _)
    .verifying(m ⇒ m == Mode.Casual.id || isAuth))
  val eloRange = optional(nonEmptyText.verifying(EloRange valid _))
  val color = nonEmptyText.verifying(Color.names contains _)
  val level = number.verifying(AiConfig.levels contains _)
  val speed = number.verifying(Config.speeds contains _)
  val eloDiff = number.verifying(FilterConfig.eloDiffs contains _)
}
