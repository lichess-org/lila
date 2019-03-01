package views.html.tournament

import play.twirl.api.Html

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

  def layout(
    title: String,
    moreJs: Html = emptyHtml,
    moreCss: String,
    side: Option[Html] = None,
    chat: Option[Frag] = None,
    underchat: Option[Frag] = None,
    chessground: Boolean = true,
    openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(implicit ctx: Context) =
    views.html.base.layout(
      title = title,
      responsive = true,
      moreCss = responsiveCssTag(moreCss),
      moreJs = moreJs,
      side = side,
      chat = chat,
      underchat = underchat,
      openGraph = openGraph,
      chessground = chessground
    )(body)

  def miniGame(pov: lila.game.Pov)(implicit ctx: Context) = frag(
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

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(title = trans.tournamentNotFound.txt()) {
      div(id := "tournament")(
        div(cls := "content_box small_box faq_page")(
          h1(trans.tournamentNotFound.frag()),
          p(trans.tournamentDoesNotExist.frag()),
          p(trans.tournamentMayHaveBeenCanceled.frag()),
          br,
          br,
          a(href := routes.Tournament.home())(trans.returnToTournamentsHomepage.frag())
        )
      )
    }.toHtml
}
