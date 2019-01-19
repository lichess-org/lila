package views.html.tournament

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object bits {

  def layout(
    title: String,
    moreJs: Html = emptyHtml,
    moreCss: Html = emptyHtml,
    side: Option[Html] = None,
    chat: Option[Frag] = None,
    underchat: Option[Frag] = None,
    chessground: Boolean = true,
    openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("tournament.css"), moreCss),
      moreJs = moreJs,
      side = side,
      chat = chat,
      underchat = underchat,
      openGraph = openGraph,
      chessground = chessground
    )(body)
}
