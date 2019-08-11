package views.html.analyse

import lidraughts.analyse.Advice.Judgement
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

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
    moreCss: Frag = emptyFrag,
    moreJs: Frag = emptyFrag,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context): Frag =
    views.html.base.layout(
      title = title,
      moreCss = moreCss,
      moreJs = moreJs,
      openGraph = openGraph,
      draughtsground = false,
      robots = false,
      zoomable = true,
      csp = defaultCsp.withPeer.some
    )(body)
}
