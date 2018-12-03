package views.html.round

import play.twirl.api.Html
import scalatags.Text.all._

import lila.api.Context
import lila.app.templating.Environment._

object layout {

  def apply(
    title: String,
    side: Frag,
    chat: Option[Html] = None,
    underchat: Option[Html] = None,
    moreJs: Html = emptyHtml,
    openGraph: Option[lila.app.ui.OpenGraph] = None,
    moreCss: Html = emptyHtml,
    chessground: Boolean = true,
    playing: Boolean = false,
    robots: Boolean = false
  )(body: Html)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      side = Some(side),
      chat = chat,
      underchat = underchat,
      openGraph = openGraph,
      moreJs = moreJs,
      moreCss = moreCss,
      chessground = chessground,
      playing = playing,
      robots = robots,
      asyncJs = true,
      zoomable = true
    )(body)
}
