package views.user.show

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import chess.Ply
import chess.format.SimpleFen

object gamesContent:

  def apply(
      u: User,
      nbs: lila.app.mashup.UserInfo.NbGames,
      pager: Paginator[Game],
      filters: lila.game.GameFilterMenu,
      filterName: String,
      notes: Map[GameId, String],
      bookmarkInfo: Map[GameId, (Ply, SimpleFen, String)]
  )(using ctx: Context) =
    frag(
      div(cls := "number-menu number-menu--tabs menu-box-pop", id := "games")(
        filters.list.map: f =>
          a(
            cls := s"nm-item to-${f.name}${(filters.current == f).so(" active")}",
            href := routes.User.games(u.username, f.name)
          )(page.userGameFilterTitle(u, nbs, f))
      ),
      nbs.crosstable
        .ifTrue(filters.current.name == "me")
        .map:
          views.game.ui.crosstable(_, none)
      ,
      div(cls := "search__result")(
        if filterName == "search" then
          if pager.nbResults > 0 then
            frag(
              div(cls := "search__status")(
                strong(trans.search.gamesFound.plural(pager.nbResults, pager.nbResults.localize))
              ),
              div(cls := "search__rows infinite-scroll")(
                views.game
                  .widgets(
                    pager.currentPageResults,
                    notes,
                    user = u.some,
                    ownerLink = ctx.is(u),
                    bookmarkInfo = bookmarkInfo
                  ),
                pagerNext(pager, np => routes.User.games(u.username, filterName, np).url)
              )
            )
          else div(cls := "search__status")(strong(trans.site.noGameFound.txt()))
        else
          div(
            cls := List(
              "games infinite-scroll" -> true,
              "now-playing center" -> (filterName == "playing" && pager.nbResults > 2)
            )
          )(
            if filterName == "playing" && pager.nbResults > 2 then
              pager.currentPageResults
                .flatMap { Pov(_, u) }
                .map: pov =>
                  views.game.mini(pov)(cls := "paginated")
            else
              views.game
                .widgets(
                  pager.currentPageResults,
                  notes,
                  user = u.some,
                  ownerLink = ctx.is(u),
                  bookmarkInfo = bookmarkInfo
                )
            ,
            pagerNext(pager, np => routes.User.games(u.username, filterName, np).url)
          )
      )
    )
