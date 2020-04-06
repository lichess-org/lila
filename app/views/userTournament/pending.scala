package views.html
package userTournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate.{th, _}
import lila.tournament.Tournament
import lila.user.User

object pending {

  private val path = "pending"

  def apply(u: User, tournaments: List[Tournament])(implicit ctx: Context) =
    bits.layout(
      u = u,
      title = s"${u.username} pending tournaments",
      path = path
    ) {
      if (tournaments.isEmpty)
        div(cls := "box-pad")(u.username, " doesn't have any pending tournaments!")
      else
        div(cls := "tournament-list")(
          table(cls := "slist")(
            thead(
              tr(
                th(cls := "count")(tournaments.size),
                th(colspan := 2)(h1(userLink(u), " pending tournaments")),
                th(trans.players())
              )
            ),
            tbody(
              tournaments.map { t =>
                tr(
                  td(cls := "icon")(iconTag(tournamentIconChar(t))),
                  td(momentFromNow(t.startsAt)),
                  td(cls := "text", dataIcon := "r")(t.nbPlayers.localize)
                )
              }
            )
          )
        )
    }
}
