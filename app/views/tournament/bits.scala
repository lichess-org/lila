package views.html.tournament

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object bits {

  def layout(
    title: String,
    moreJs: Html = emptyHtml,
    moreCss: Html = emptyHtml,
    side: Option[Html] = None,
    chat: Option[Frag] = None,
    underchat: Option[Frag] = None,
    draughtsground: Boolean = true,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      moreCss = frag(cssTag("tournament.css"), moreCss),
      moreJs = moreJs,
      side = side,
      chat = chat,
      underchat = underchat,
      openGraph = openGraph,
      draughtsground = draughtsground
    )(body)

  def miniGame(pov: lidraughts.game.Pov)(implicit ctx: Context) = frag(
    gameFen(pov),
    div(cls := "vstext")(
      playerUsername(pov.opponent, withRating = true, withTitle = true),
      br,
      span(cls := List(
        "result" -> true,
        "win" -> ~pov.win,
        "loss" -> ~pov.loss
      ))(gameEndStatus(pov.game))
    )
  )
}
