package lila.app
package templating

import chess.{ Mode, Variant, Speed }
import lila.api.Context
import lila.tournament.System

trait SetupHelper { self: I18nHelper =>

  def translatedModeChoices(implicit ctx: Context) = List(
    (Mode.Casual.id.toString, trans.casual.str(), none),
    (Mode.Rated.id.toString, trans.rated.str(), none)
  )

  def translatedSystemChoices(implicit ctx: Context) = List(
    System.Arena.id.toString -> "Arena",
    System.Swiss.id.toString -> "Swiss [beta]"
  )

  private def variantTuple(variant: Variant): (String, String, Option[String]) =
    (variant.id.toString, variant.name, variant.title.some)

  def translatedVariantChoices(implicit ctx: Context) = List(
    (Variant.Standard.id.toString, trans.standard.str(), Variant.Standard.title.some),
    variantTuple(Variant.Chess960)
  )

  def translatedVariantChoicesWithVariants(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(Variant.KingOfTheHill) :+
      variantTuple(Variant.ThreeCheck)

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      variantTuple(Variant.FromPosition)

  def translatedVariantChoicesWithFenAndKingOfTheHill(implicit ctx: Context) =
    translatedVariantChoicesWithFen(ctx) :+
      variantTuple(Variant.KingOfTheHill)

  def translatedVariantChoicesWithVariantsAndFen(implicit ctx: Context) =
    translatedVariantChoicesWithVariants :+
      variantTuple(Variant.FromPosition)

  def translatedSpeedChoices(implicit ctx: Context) = Speed.all map { s =>
    (s.id.toString, {
      (s.range.min, s.range.max) match {
        case (0, y)            => s.toString + " - " + trans.lessThanNbMinutes(y / 60 + 1)
        case (x, Int.MaxValue) => trans.unlimited.str()
        case (x, y)            => s.toString + " - " + trans.xToYMinutes(x / 60, y / 60 + 1)
      }
    }, none)
  }
}
