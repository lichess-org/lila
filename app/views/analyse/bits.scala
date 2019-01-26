package views.html.analyse

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.analyse.Advice.Judgement

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
    side: Option[Frag] = None,
    chat: Option[Frag] = None,
    underchat: Option[Frag] = None,
    moreCss: Html = emptyHtml,
    moreJs: Html = emptyHtml,
    openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Html)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = title,
      side = side.map(_.toHtml),
      chat = chat,
      underchat = underchat,
      moreCss = moreCss,
      moreJs = moreJs,
      openGraph = openGraph,
      chessground = false,
      robots = false,
      zoomable = true
    )(body)
}
