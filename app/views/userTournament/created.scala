package views.html
package userTournament

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.user.User

import controllers.routes

object created:

  private val path = "created"

  def apply(u: User, pager: Paginator[lila.tournament.Tournament])(using PageContext) =
    bits.layout(
      u = u,
      title = s"${u.username} created tournaments",
      path = path,
      moreJs = infiniteScrollTag
    ) {
      if pager.nbResults == 0 then div(cls := "box-pad")(u.username, " hasn't created any tournament yet!")
      else
        div(cls := "tournament-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 2)(h1(userLink(u, withOnline = true), " tournaments")),
                th(trans.winner()),
                th(trans.players())
              )
            ),
            tbody(cls := "infinite-scroll")(
              pager.currentPageResults.map { t =>
                tr(cls := "paginated")(
                  td(cls := "icon")(iconTag(tournamentIcon(t))),
                  views.html.tournament.finishedList.header(t),
                  td(momentFromNow(t.startsAt)),
                  td(cls := "winner")(
                    t.winnerId.isDefined option userIdLink(t.winnerId, withOnline = false)
                  ),
                  td(cls := "text", dataIcon := licon.User)(t.nbPlayers.localize)
                )
              },
              pagerNextTable(pager, np => routes.UserTournament.path(u.username, path, np).url)
            )
          )
        )
    }
