package lila.app
package templating

import chess.{ Mode, Variant, Speed }
import lila.setup._
import lila.api.Context

trait SetupHelper { self: I18nHelper =>

  def translatedModeChoices(implicit ctx: Context) = List(
    Mode.Casual.id.toString -> trans.casual.str(),
    Mode.Rated.id.toString -> trans.rated.str()
  )

  def translatedVariantChoices(implicit ctx: Context) = List(
    Variant.Standard.id.toString -> trans.standard.str(),
    Variant.Chess960.id.toString -> Variant.Chess960.name.capitalize
  )

  def translatedVariantChoicesWithFen(implicit ctx: Context) =
    translatedVariantChoices(ctx) :+ (Variant.FromPosition.id.toString -> "FEN")

  def translatedSpeedChoices(implicit ctx: Context) = Speed.all map { s =>
    s.id.toString -> {
      (s.range.min, s.range.max) match {
        case (0, y)            => s.toString + " - " + trans.lessThanNbMinutes(y/60 + 1)
        case (x, Int.MaxValue) => trans.unlimited.str()
        case (x, y)            => s.toString + " - " + trans.xToYMinutes(x/60, y/60 + 1)
      }
    }
  }
}
