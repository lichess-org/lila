package lila.app
package templating

import chess.{ Mode, Speed }
import lila.setup.TimeMode
import lila.api.Context
import lila.tournament.System

trait SetupHelper { self: I18nHelper =>

  def translatedTimeModeChoices(implicit ctx: Context) = List(
    (TimeMode.RealTime.id.toString, trans.realTime.str(), none),
    (TimeMode.Correspondence.id.toString, trans.correspondence.str(), none),
    (TimeMode.Unlimited.id.toString, trans.unlimited.str(), none)
  )

  def translatedModeChoices(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, trans.casual.str(), none),
    (Mode.Rated.id.toString, trans.rated.str(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena",
    System.Swiss.id.toString -> "Swiss [beta]"
  )

  private def variantTuple(variant: chess.variant.Variant)(implicit ctx: Context): (String, String, Option[String]) =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (chess.variant.Standard.id.toString, trans.standard.str(), chess.variant.Standard.title.some),
    variantTuple(chess.variant.Chess960)
  )

  def translatedVariantChoicesWithVariants(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.KingOfTheHill) :+
      variantTuple(chess.variant.ThreeCheck) :+
      variantTuple(chess.variant.Antichess) :+
      variantTuple(chess.variant.Atomic)

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.FromPosition)

  def translatedVariantChoicesWithFenAndKingOfTheHill(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(chess.variant.KingOfTheHill) :+
      variantTuple(chess.variant.FromPosition)

  def translatedVariantChoicesWithVariantsAndFen(implicit ctx: Context) =
    translatedVariantChoicesWithVariants :+
      variantTuple(chess.variant.FromPosition)

  def translatedSpeedChoices(implicit ctx: Context) = Speed.limited map { s =>
    (s.id.toString, {
      (s.range.min, s.range.max) match {
        case (0, y)            => s.toString + " - " + trans.lessThanNbMinutes(y / 60 + 1)
        case (x, y)            => s.toString + " - " + trans.xToYMinutes(x / 60, y / 60 + 1)
      }
    }, none)
  }
}
