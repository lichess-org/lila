package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.game.Pov
import lila.user.User
import lila.evaluation.PlayerAssessment

object games {

  private val sortNumberTh = th(attr("data-sort-method") := "number")
  private val dataSort     = attr("data-sort")

  def apply(user: User, games: List[(Pov, Option[PlayerAssessment])])(implicit ctx: Context) =
    views.html.base.layout(
      title = s"${user.username} games",
      moreCss = cssTag("mod.games"),
      moreJs = jsModule("mod.games")
    ) {
      main(cls := "mod-games page-menu")(
        st.nav(cls := "page-menu__menu subnav")(
          "menu of sorts"
        ),
        div(cls := "mod-games page-menu__content box")(
          h1(userLink(user), " games"),
          table(cls := "mod-games game-list slist")(
            thead(
              tr(
                th("Game"),
                th("Perf"),
                td,
                th("Moves"),
                th("Result"),
                th("ACPL", br, "(Avg ± SD)"),
                th("Times", br, "(Avg ± SD)"),
                th("Blur"),
                th("Date")
              )
            ),
            tbody(
              games.map { case (pov, assessment) =>
                tr(
                  td(
                    a(href := routes.Round.watcher(pov.gameId, pov.color.name))(playerUsername(pov.opponent))
                  ),
                  td(
                    pov.game.perfType.map { pt =>
                      iconTag(pt.iconChar)
                    },
                    shortClockName(pov.game.clock.map(_.config))
                  ),
                  td(
                    pov.game.tournamentId map { tourId =>
                      a(
                        dataIcon := "g",
                        href := routes.Tournament.show(tourId).url,
                        title := tournamentIdToName(tourId)
                      )
                    }
                  ),
                  sortNumberTh(dataSort := pov.game.playedTurns)(pov.game.playedTurns),
                  sortNumberTh(dataSort := ~pov.player.ratingDiff)(
                    pov.win match {
                      case Some(true)  => goodTag(cls := "result")("1")
                      case Some(false) => badTag(cls := "result")("0")
                      case None        => span(cls := "result")("½")
                    },
                    pov.player.ratingDiff match {
                      case Some(d) if d > 0 => goodTag(s"+$d")
                      case Some(d) if d < 0 => badTag(d)
                      case _                => span("-")
                    }
                  ),
                  assessment match {
                    case Some(ass) =>
                      frag(
                        td(s"${ass.sfAvg} ± ${ass.sfSd}"),
                        td(
                          s"${ass.mtAvg / 10} ± ${ass.mtSd / 10}",
                          ~ass.mtStreak ?? frag(br, "STREAK")
                        ),
                        td(
                          s"${ass.blurs}%",
                          ass.blurStreak.filter(8 <=) map { s =>
                            frag(br, s"STREAK $s/12")
                          }
                        )
                      )
                    case _ => td(colspan := 3)
                  },
                  td(dataSort := pov.game.movedAt.getMillis.toString)(momentFromNowServer(pov.game.movedAt))
                )
              }
            )
          )
        )
      )
    }
}
