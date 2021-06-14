package views.html
package user

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.game.FavoriteOpponents

import controllers.routes

object opponents {
  def apply(u: User, sugs: List[lila.relation.Related])(implicit ctx: Context) =
    relation.bits.layout(s"${u.username} • ${trans.favoriteOpponents.txt()}")(
      h1(
        a(href := routes.User.show(u.username), dataIcon := "", cls := "text"),
        trans.favoriteOpponents(),
        " (",
        trans.nbGames.pluralSame(FavoriteOpponents.gameLimit),
        ")"
      ),
      table(cls := "slist")(
        tbody(
          if (sugs.nonEmpty) sugs.map { r =>
            tr(
              td(userLink(r.user)),
              td(showBestPerf(r.user)),
              td(
                r.nbGames.filter(_ > 0).map { nbGames =>
                  a(href := s"${routes.User.games(u.username, "search")}?players.b=${r.user.username}")(
                    trans.nbGames.pluralSame(nbGames)
                  )
                }
              ),
              td(relation.actions(r.user.id, r.relation, followable = r.followable, blocked = false))
            )
          }
          else tr(td(trans.none()))
        )
      )
    )
}
