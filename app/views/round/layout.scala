package views.html.round

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object layout {

  def apply(
    title: String,
    side: Option[Frag],
    chat: Option[Frag] = None,
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
      side = side.map(_.toHtml),
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
