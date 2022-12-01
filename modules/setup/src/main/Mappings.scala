package lila.setup

import chess.format.Fen
import chess.Mode
import chess.{ variant as V }
import play.api.data.format.Formats.*
import play.api.data.Forms.*

import lila.common.Days
import lila.common.Form.{ *, given }
import lila.game.GameRule
import lila.lobby.Color
import lila.rating.RatingRange

private object Mappings:

  val variant                   = number.verifying(Config.variants contains _)
  val variantWithFen            = number.verifying(Config.variantsWithFen contains _)
  val aiVariants                = number.verifying(Config.aiVariants contains _)
  val variantWithVariants       = number.verifying(Config.variantsWithVariants contains _)
  val variantWithFenAndVariants = number.verifying(Config.variantsWithFenAndVariants contains _)
  val boardApiVariants = Set(
    V.Standard.key,
    V.Chess960.key,
    V.Crazyhouse.key,
    V.KingOfTheHill.key,
    V.ThreeCheck.key,
    V.Antichess.key,
    V.Atomic.key,
    V.Horde.key,
    V.RacingKings.key
  )
  val boardApiVariantKeys      = text.verifying(boardApiVariants contains _)
  val time                     = of[Double].verifying(HookConfig validateTime _)
  val increment                = number.verifying(HookConfig validateIncrement _)
  val daysChoices              = List(1, 2, 3, 5, 7, 10, 14).map(Days(_))
  val days                     = of[Days].verifying(mustBeOneOf(daysChoices), daysChoices.contains)
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
  val fenField = optional {
    import lila.common.Form.fen.{ mapping, truncateMoveNumber }
    mapping.transform[Fen](truncateMoveNumber, identity)
  }
  val gameRules = lila.common.Form.strings
    .separator(",")
    .verifying(_.forall(GameRule.byKey.contains))
    .transform[Set[GameRule]](rs => rs.flatMap(GameRule.byKey.get).toSet, _.map(_.key).toList)
