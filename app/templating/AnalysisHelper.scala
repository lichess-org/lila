package lila.app
package templating

import chess.format.Nag
import play.api.data._
import play.twirl.api.Html

import lila.api.Context

trait AnalysisHelper { self: I18nHelper with SecurityHelper =>

  def nagName(nag: Nag)(implicit ctx: Context) = nag match {
    case Nag.Blunder    => trans.blunders()
    case Nag.Mistake    => trans.mistakes()
    case Nag.Inaccuracy => trans.inaccuracies()
    case nag            => nag.toString
  }
}
