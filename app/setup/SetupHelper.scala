package lila
package setup

import http.Context
import i18n.I18nHelper
import chess.{ Mode, Variant }

trait SetupHelper { self: I18nHelper ⇒

  def translatedModeChoices(implicit ctx: Context) = List(
    Mode.Casual.id.toString -> trans.casual.str(),
    Mode.Rated.id.toString -> trans.rated.str()
  )

  def translatedVariantChoices(implicit ctx: Context) = List(
    Variant.Standard.id.toString -> trans.standard.str(),
    Variant.Chess960.id.toString -> Variant.Chess960.name
  )
}
