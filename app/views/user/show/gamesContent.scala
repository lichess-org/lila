package views.html.user.show

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.game.{ Game, Pov }
import lila.user.User

object gamesContent {

  def apply(
      u: User,
      nbs: lila.app.mashup.UserInfo.NbGames,
      pager: Paginator[Game],
      filters: lila.app.mashup.GameFilterMenu,
      filterName: String,
      notes: Map[Game.ID, String]
  )(implicit ctx: Context) =
    frag(
      div(cls := "number-menu number-menu--tabs menu-box-pop", id := "games")(
        filters.list.map { f =>
          a(
            cls := s"nm-item to-${f.name}${(filters.current == f) ?? " active"}",
            href := routes.User.games(u.username, f.name)
          )(userGameFilterTitle(u, nbs, f))
        }
      ),
      nbs.crosstable.ifTrue(filters.current.name == "me").map {
        views.html.game.crosstable(_, none)
      },
      div(cls := "search__result")(
        if (filterName == "search") {
          if (pager.nbResults > 0)
            frag(
              div(cls := "search__status")(
                strong(pager.nbResults.localize, " games found")
              ),
              div(cls := "search__rows infinite-scroll")(
                views.html.game.widgets(pager.currentPageResults, notes, user = u.some, ownerLink = ctx is u),
                pagerNext(pager, np => routes.User.games(u.username, filterName, np).url)
              )
            )
          else
            div(cls := "search__status")(strong("No game found"))
        } else
          div(
            cls := List(
              "games infinite-scroll" -> true,
              "now-playing center"    -> (filterName == "playing" && pager.nbResults > 2)
            )
          )(
            if (filterName == "playing" && pager.nbResults > 2)
              pager.currentPageResults.flatMap { Pov(_, u) }.map { pov =>
                views.html.game.mini(pov)(ctx)(cls := "paginated")
              }
            else
              views.html.game.widgets(pager.currentPageResults, notes, user = u.some, ownerLink = ctx is u),
            pagerNext(pager, np => routes.User.games(u.username, filterName, np).url)
          )
      )
    )
}
