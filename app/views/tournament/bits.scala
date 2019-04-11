package views.html.tournament

import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object bits {

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

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournamentNotFound.txt()
    ) {
        main(cls := "page-small box box-pad")(
          h1(trans.tournamentNotFound.frag()),
          p(trans.tournamentDoesNotExist.frag()),
          p(trans.tournamentMayHaveBeenCanceled.frag()),
          br,
          br,
          a(href := routes.Tournament.home())(trans.returnToTournamentsHomepage.frag())
        )
      }
}
