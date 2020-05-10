package views.html
package userTournament

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.paginator.Paginator
import lidraughts.user.User

import controllers.routes

object created {

  private val path = "created"

  def apply(u: User, pager: Paginator[lidraughts.tournament.Tournament])(implicit ctx: Context) =
    bits.layout(
      u = u,
      title = s"${u.username} created tournaments",
      path = path,
      moreJs = infiniteScrollTag
    ) {
      if (pager.nbResults == 0)
        div(cls := "box-pad")(u.username, " hasn't created any tournament yet!")
      else
        div(cls := "tournament-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 2)(h1(userLink(u, withOnline = true), trans.xTournaments(""))),
                th(trans.winner()),
                th(trans.players())
              )
            ),
            tbody(cls := "infinitescroll")(
              pager.nextPage.map { np =>
                tr(
                  th(cls := "pager none")(
                    a(rel := "next", href := routes.UserTournament.path(u.username, path, np))(trans.next())
                  )
                )
              },
              pager.currentPageResults.map { t =>
                tr(cls := "paginated")(
                  td(cls := "icon")(iconTag(tournamentIconChar(t))),
                  views.html.tournament.finishedPaginator.header(t),
                  td(momentFromNow(t.startsAt)),
                  td(cls := "winner")(
                    t.winnerId.isDefined option userIdLink(t.winnerId, withOnline = false)
                  ),
                  td(cls := "text", dataIcon := "r")(t.nbPlayers.localize)
                )
              }
            )
          )
        )
    }
}