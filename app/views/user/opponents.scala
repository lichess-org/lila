package views.html
package user

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User

import controllers.routes

object opponents {
  def apply(u: User, sugs: List[lidraughts.relation.Related])(implicit ctx: Context) =
    relation.bits.layout(s"${u.username} • ${trans.favoriteOpponents.txt()}")(
      h1(
        userLink(u, withOnline = false),
        " ",
        trans.favoriteOpponents()
      ),
      table(cls := "slist")(
        tbody(
          if (sugs.nonEmpty) sugs.map { r =>
            tr(
              td(userLink(r.user)),
              td(showBestPerf(r.user)),
              td(
                r.nbGames.filter(_ > 0).map { nbGames =>
                  a(href := s"${routes.User.games(u.username, "search")}?players.b=${r.user.username}", title := "Games count over your last 1000 games")(
                    trans.nbGames.pluralSame(nbGames)
                  )
                }
              ),
              td(relation.actions(r.user.id, r.relation, followable = r.followable, blocked = false))
            )
          }
          else tr(td("None found."))
        )
      )
    )
}
