package views.html.round

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object layout {

  def apply(
    title: String,
    side: Option[Frag],
    chat: Option[Frag] = None,
    underchat: Option[Html] = None,
    moreJs: Html = emptyHtml,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None,
    moreCss: Html = emptyHtml,
    draughtsground: Boolean = true,
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
      draughtsground = draughtsground,
      playing = playing,
      robots = robots,
      asyncJs = true,
      zoomable = true
    )(body)
}
