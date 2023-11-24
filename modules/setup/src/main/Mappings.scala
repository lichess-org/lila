package lila.setup

import play.api.data.Forms._
import play.api.data.format.Formats._

import shogi.Mode
import shogi.{ variant => V }
import lila.rating.RatingRange
import lila.lobby.Color

private object Mappings {

  val variants   = number.verifying(Config.variants contains _)
  val aiVariants = number.verifying(Config.aiVariants contains _)
  val boardApiVariants = Set(
    V.Standard.key,
    V.Minishogi.key,
    V.Chushogi.key,
    V.Annanshogi.key,
    V.Kyotoshogi.key,
    V.Checkshogi.key
  )
  val boardApiVariantKeys      = text.verifying(boardApiVariants contains _)
  val time                     = of[Double].verifying(HookConfig validateTime _)
  val increment                = number.verifying(HookConfig validateIncrement _)
  val byoyomi                  = number.verifying(HookConfig validateByoyomi _)
  val periods                  = number.verifying(HookConfig validatePeriods _)
  val days                     = number(min = 1, max = 14)
  def timeMode                 = number.verifying(TimeMode.ids contains _)
  def mode(withRated: Boolean) = optional(rawMode(withRated))
  def rawMode(withRated: Boolean) =
    number
      .verifying(HookConfig.modes contains _)
      .verifying(m => m == Mode.Casual.id || withRated)
  val ratingRange = text.verifying(RatingRange valid _)
  val color       = text.verifying(Color.names contains _)
  val level       = number.verifying(AiConfig.levels contains _)
  val speed       = number.verifying(Config.speeds contains _)
  val sfenField   = optional(nonEmptyText.transform[String](_.replace("_", " "), identity))
}
