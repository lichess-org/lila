package views.html
package userTournament

import lila.api.Context
import lila.user.User
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator

import controllers.routes

object list {

  def apply(u: User, path: String, pager: Paginator[lila.tournament.LeaderboardApi.TourEntry], title: String, count: String)(implicit ctx: Context) =
    if (pager.nbResults == 0) {
      div(cls := "box-pad")(u.username, " hasn't played in any tournament yet!")
    } else {
      div(cls := "tournament-list")(
        table(cls := "slist")(
          thead(
            tr(
              th(cls := "count")(count),
              th(h1(userLink(u, withOnline = true), " tournaments")),
              th("Games"),
              th("Points"),
              th("Rank")
            )
          ),
          tbody(cls := "infinitescroll")(
            pagerNextTable(pager, np => routes.UserTournament.path(u.username, path, np).url),
            pager.currentPageResults.map { e =>
              tr(cls := List("paginated" -> true, "scheduled" -> e.tour.isScheduled))(
                td(cls := "icon")(iconTag(tournamentIconChar(e.tour))),
                td(cls := "header")(
                  a(href := routes.Tournament.show(e.tour.id))(
                    span(cls := "name")(e.tour.fullName),
                    span(cls := "setup")(
                      e.tour.clock.show, " • ",
                      if (e.tour.variant.exotic) e.tour.variant.name else e.tour.perfType.map(_.name), " • ",
                      momentFromNow(e.tour.startsAt)
                    )
                  )
                ),
                td(cls := "games")(e.entry.nbGames),
                td(cls := "score")(e.entry.score),
                td(cls := "rank")(strong(e.entry.rank), " / ", e.tour.nbPlayers)
              )
            }
          )
        )
      )
    }
}
