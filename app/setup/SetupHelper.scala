package lila
package setup

import http.Context
import i18n.I18nHelper
import templating.StringHelper
import chess.{ Mode, Variant, Speed }

trait SetupHelper { self: I18nHelper ⇒

  def translatedModeChoices(implicit ctx: Context) = List(
    Mode.Casual.id.toString -> trans.casual.str(),
    Mode.Rated.id.toString -> trans.rated.str()
  )

  def translatedVariantChoices(implicit ctx: Context) = List(
    Variant.Standard.id.toString -> trans.standard.str(),
    Variant.Chess960.id.toString -> StringHelper.ucFirst(Variant.Chess960.name)
  )

  def translatedSpeedChoices(implicit ctx: Context) = Speed.all map { s ⇒
    s.id.toString -> (s.toString + " - " + StringHelper.ucFirst(s.name))
  }

  def eloDiffChoices(elo: Int)(implicit ctx: Context) = FilterConfig.eloDiffs map { diff ⇒
    diff -> (diff == 0).fold(
      trans.eloRange.str(), 
      "%d - %d (+-%d)".format(elo - diff, elo + diff, diff)
    )
  }
}
