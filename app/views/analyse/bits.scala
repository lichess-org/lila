package views.html.analyse

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.analyse.Advice.Judgement

object bits {

  val dataPanel = attr("data-panel")

  def judgmentName(judgment: Judgement)(implicit ctx: Context): String = judgment match {
    case Judgement.Blunder => trans.blunders.txt()
    case Judgement.Mistake => trans.mistakes.txt()
    case Judgement.Inaccuracy => trans.inaccuracies.txt()
    case j => j.toString
  }

  def layout(
    title: String,
    side: Option[Html] = None,
    chat: Option[Html] = None,
    underchat: Option[Html] = None,
    moreCss: Html = emptyHtml,
    moreJs: Html = emptyHtml,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None,
    draughtsground: Boolean
  )(body: Html)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = title,
      side = side,
      chat = chat,
      underchat = underchat,
      moreCss = moreCss,
      moreJs = moreJs,
      openGraph = openGraph,
      draughtsground = draughtsground,
      robots = false,
      zoomable = true
    )(body)
}
