package lila.app
package templating

import lila.user.Context
import chess.format.Nag

import play.api.data._
import play.api.templates.Html

trait AnalysisHelper { self: I18nHelper ⇒

  def nagName(nag: Nag)(implicit ctx: Context) = nag match {
    case Nag.Blunder    ⇒ trans.blunders()
    case Nag.Mistake    ⇒ trans.mistakes()
    case Nag.Inaccuracy ⇒ trans.inaccuracies()
  }
}
