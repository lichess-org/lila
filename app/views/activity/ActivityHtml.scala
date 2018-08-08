package views.html.activity

import play.twirl.api.Html

import lidraughts.activity.model._
import lidraughts.api.Context
import lidraughts.i18n.{ I18nKey, I18nKeys => trans }
import lidraughts.app.templating.Environment._

object ActivityHtml extends lidraughts.Lidraughtsisms {

  def scoreHtml(s: Score)(implicit ctx: Context) = Html {
    s"""<score>${scorePart("win", s.win, trans.nbWins)}${scorePart("draw", s.draw, trans.nbDraws)}${scorePart("loss", s.loss, trans.nbLosses)}</score>"""
  }

  def ratingProgHtml(r: RatingProg)(implicit ctx: Context) = Html {
    val prog = showProgress(r.diff, withTitle = false)
    s"""<rating>${r.after.value}$prog</rating>"""
  }

  private def scorePart(tag: String, p: Int, name: I18nKey)(implicit ctx: Context) =
    if (p == 0) ""
    else s"""<$tag>${wrapNumber(name.pluralSameTxt(p))}</$tag>"""

  private val wrapNumberRegex = """(\d+)""".r
  private def wrapNumber(str: String) =
    wrapNumberRegex.replaceAllIn(str, "<strong>$1</strong>")
}
