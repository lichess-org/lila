package views.html.user.show

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.game.{ Game, Pov }
import lila.user.User

import controllers.routes

object gamesContent {

  def apply(
    u: User,
    nbs: lila.app.mashup.UserInfo.NbGames,
    pager: Paginator[Game],
    filters: lila.app.mashup.GameFilterMenu,
    filterName: String
  )(implicit ctx: Context) = frag(

    div(cls := "tabs", id := "games")(
      filters.list.map { f =>
        a(
          cls := s"intertab to_${f.name}${(filters.current == f) ?? " active"}",
          href := routes.User.games(u.username, f.name)
        )(userGameFilterTitle(u, nbs, f))
      }
    ),
    filters.current.name == "me" option nbs.crosstable.map { c =>
      div(cls := "crosstable")(views.html.game.crosstable(c, none))
    },
    div(cls := "search_result")(
      if (filterName == "search") {
        if (pager.nbResults > 0) frag(
          div(cls := "search_status")(
            strong(pager.nbResults.localize, " games found"),
            " • ",
            a(rel := "nofollow", href := routes.User.games(u.username, filterName))("Permalink"),
            " • "
          ),
          div(cls := "search_infinitescroll")(
            pager.nextPage.map { n =>
              div(cls := "pager none")(a(rel := "next", href := routes.User.games(u.username, filterName, n))("Next"))
            } getOrElse div(cls := "none"),
            views.html.game.widgets(pager.currentPageResults, user = u.some, ownerLink = ctx is u)
          )
        )
        else div(cls := "search_status")(
          "No game found - ",
          a(href := routes.User.games(u.username, filterName))("Permalink")
        )
      } else
        div(cls := List(
          "games infinitescroll" -> true,
          "game_list playing center" -> (filterName == "playing" && pager.nbResults > 2)
        ))(
          pager.nextPage.map { np =>
            div(cls := "pager none")(a(href := routes.User.games(u.username, filterName, np))("Next"))
          },
          if (filterName == "playing" && pager.nbResults > 2)
            pager.currentPageResults.flatMap { Pov(_, u) }.map { p =>
            div(cls := "paginated")(
              gameFen(p),
              views.html.game.bits.vstext(p)(ctx.some)
            )
          }
          else views.html.game.widgets(pager.currentPageResults, user = u.some, ownerLink = ctx is u)
        )
    )
  )
}
