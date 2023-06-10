package views.html
package userTournament

import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.user.User

import controllers.routes

object list {

  def apply(
      u: User,
      path: String,
      pager: Paginator[lila.tournament.LeaderboardApi.TourEntry],
      count: String
  )(implicit lang: Lang) =
    if (pager.nbResults == 0)
      div(cls := "box-pad")(u.username, " hasn't played in any tournament yet!")
    else
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
                    span(cls := "name")(e.tour.name()),
                    span(cls := "setup")(
                      e.tour.clock.show,
                      " - ",
                      if (!e.tour.variant.standard) variantName(e.tour.variant)
                      else e.tour.perfType.map(_.trans),
                      " - ",
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
