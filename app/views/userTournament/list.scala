package views.html
package userTournament

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }
import scalalib.paginator.Paginator

object list:

  def apply(
      u: User,
      path: String,
      pager: Paginator[lila.tournament.LeaderboardApi.TourEntry],
      count: String
  )(using Translate) =
    if pager.nbResults == 0 then div(cls := "box-pad")(trans.site.nothingToSeeHere())
    else
      div(cls := "tournament-list")(
        table(cls := "slist")(
          thead(
            tr(
              th(cls := "count")(count),
              th(h1(frag(userLink(u, withOnline = true), " • ", trans.site.tournaments()))),
              th(trans.site.games()),
              th(trans.site.points()),
              th(trans.site.rank())
            )
          ),
          tbody(cls := "infinite-scroll")(
            pager.currentPageResults.map { e =>
              tr(cls := List("paginated" -> true, "scheduled" -> e.tour.isScheduled))(
                td(cls := "icon")(iconTag(tournamentIcon(e.tour))),
                td(cls := "header")(
                  a(href := routes.Tournament.show(e.tour.id))(
                    span(cls := "name")(e.tour.name()),
                    span(cls := "setup")(
                      e.tour.clock.show,
                      " • ",
                      if e.tour.variant.exotic then e.tour.variant.name else e.tour.perfType.trans,
                      " • ",
                      momentFromNow(e.tour.startsAt)
                    )
                  )
                ),
                td(cls := "games")(e.entry.nbGames),
                td(cls := "score")(e.entry.score),
                td(cls := "rank")(strong(e.entry.rank), " / ", e.tour.nbPlayers)
              )
            },
            pagerNextTable(pager, np => routes.UserTournament.path(u.username, path, np).url)
          )
        )
      )
