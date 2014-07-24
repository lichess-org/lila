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

  def translatedVariantChoices(implicit ctx: Context) = List(
    (Variant.Standard.id.toString, trans.standard.str(), variantDesc.Standard.some),
    (Variant.Chess960.id.toString, Variant.Chess960.name.capitalize, variantDesc.Chess960.some)
  )

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      (Variant.FromPosition.id.toString, "FEN", variantDesc.Position.some)

  def translatedVariantChoicesWithCenterAndFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+
      (Variant.Center.id.toString, "Center", variantDesc.Center.some) :+
      (Variant.FromPosition.id.toString, "FEN", variantDesc.Position.some)

  def translatedSpeedChoices(implicit ctx: Context) = Speed.all map { s =>
    (s.id.toString, {
      (s.range.min, s.range.max) match {
        case (0, y)            => s.toString + " - " + trans.lessThanNbMinutes(y / 60 + 1)
        case (x, Int.MaxValue) => trans.unlimited.str()
        case (x, y)            => s.toString + " - " + trans.xToYMinutes(x / 60, y / 60 + 1)
      }
    }, none)
  }

  object variantDesc {
    val Standard = "Standard rules of chess (FIDE)"
    val Chess960 = "Starting position of the home rank pieces is randomized"
    val Center = "Bring your king to the center to win the game"
    val Position = "Custom starting position"
  }
}
