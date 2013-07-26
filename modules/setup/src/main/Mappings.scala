package lila.setup

import play.api.data.Forms._

import chess.format.Forsyth
import chess.Mode
import lila.common.EloRange
import lila.lobby.Color

object Mappings {

  val variant = number.verifying(Config.variants contains _)
  val variantWithFen = number.verifying(Config.variantsWithFen contains _)
  val time = number.verifying(HookConfig.times contains _)
  val increment = number.verifying(HookConfig.increments contains _)
  def mode(isAuth: Boolean) = optional(rawMode(isAuth))
  def rawMode(isAuth: Boolean) = number
    .verifying(HookConfig.modes contains _)
    .verifying(m ⇒ m == Mode.Casual.id || isAuth)
  val eloRange = optional(nonEmptyText.verifying(EloRange valid _))
  val color = nonEmptyText.verifying(Color.names contains _)
  val level = number.verifying(AiConfig.levels contains _)
  val speed = number.verifying(Config.speeds contains _)

  def fen(strict: Boolean) = optional {
    nonEmptyText verifying { source ⇒ ~(Forsyth <<< source).map(_.situation playable strict) }
  }
}
