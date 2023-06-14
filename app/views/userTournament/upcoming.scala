package views.html
package userTournament

import lila.api.WebContext
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.user.User

object upcoming:

  def apply(u: User, pager: Paginator[lila.tournament.Tournament])(using WebContext) =
    bits.layout(
      u = u,
      title = s"${u.username} upcoming tournaments",
      path = "upcoming"
    ) {
      if (pager.nbResults == 0)
        div(cls := "box-pad")(u.username, " hasn't joined any tournament yet!")
      else
        div(cls := "tournament-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(pager.nbResults),
                th(colspan := 2)(h1(userLink(u, withOnline = true), " upcoming tournaments")),
                th(trans.players())
              )
            ),
            tbody(
              pager.currentPageResults.map { t =>
                tr(
                  td(cls := "icon")(iconTag(tournamentIcon(t))),
                  views.html.tournament.finishedList.header(t),
                  td(momentFromNow(t.startsAt)),
                  td(cls := "text", dataIcon := licon.User)(t.nbPlayers.localize)
                )
              }
            )
          )
        )
    }
