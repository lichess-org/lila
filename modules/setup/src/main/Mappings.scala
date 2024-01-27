package lila.setup

import chess.format.Fen
import chess.{ variant as V, Mode, Clock }
import play.api.data.format.Formats.doubleFormat
import play.api.data.Forms.*
import play.api.data.Mapping

import lila.common.Days
import lila.common.Form.{ *, given }
import lila.game.GameRule
import lila.lobby.Color
import lila.rating.RatingRange

private object Mappings:

  val variant                   = typeIn(Config.variants.toSet)
  val variantWithFen            = typeIn(Config.variantsWithFen.toSet)
  val aiVariants                = typeIn(Config.aiVariants.toSet)
  val variantWithVariants       = typeIn(Config.variantsWithVariants.toSet)
  val variantWithFenAndVariants = typeIn(Config.variantsWithFenAndVariants.toSet)
  val boardApiVariants          = V.Variant.list.all.view.filterNot(_.fromPosition).map(_.key).toSet
  val boardApiVariantKeys       = typeIn(boardApiVariants)
  val time                      = of[Double].verifying(HookConfig validateTime _)
  val increment                 = of[Clock.IncrementSeconds].verifying(HookConfig validateIncrement _)
  val daysChoices               = Days from List(1, 2, 3, 5, 7, 10, 14)
  val days                      = typeIn(daysChoices.toSet)
  def timeMode                  = number.verifying(TimeMode.ids contains _)
  def mode(withRated: Boolean)  = optional(rawMode(withRated))
  def rawMode(withRated: Boolean) =
    number
      .verifying(HookConfig.modes contains _)
      .verifying(_ == Mode.Casual.id || withRated)
  val ratingRange = text.verifying(RatingRange valid _)
  val color       = text.verifying(Color.names contains _)
  val level       = number.verifying(AiConfig.levels contains _)
  val speed       = number.verifying(Config.speeds contains _)
  val fenField = optional:
    import lila.common.Form.fen.{ mapping, truncateMoveNumber }
    mapping.transform[Fen.Epd](truncateMoveNumber, identity)
  val gameRules: Mapping[Set[GameRule]] = lila.common.Form.strings
    .separator(",")
    .verifying(_.forall(GameRule.byKey.contains))
    .transform[Set[GameRule]](rs => rs.flatMap(GameRule.byKey.get).toSet, _.map(_.key).toList)
