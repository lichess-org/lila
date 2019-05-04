package views.html.search

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.gameSearch.{ Query, Sorting }
import lila.user.User

import controllers.routes

object index {

  def apply(form: Form[_], paginator: Option[Paginator[lila.game.Game]] = None, nbGames: Int)(implicit ctx: Context) = {
    val commons = bits of form
    import commons._
    views.html.base.layout(
      title = trans.advancedSearch.txt(),
      moreJs = frag(
        jsTag("search.js"),
        infiniteScrollTag
      ),
      moreCss = cssTag("search"),
      openGraph = lila.app.ui.OpenGraph(
        title = s"Search in ${nbGames.localize} chess games",
        url = s"$netBaseUrl${routes.Search.index().url}",
        description = s"Search in ${nbGames.localize} chess games using advanced criterions"
      ).some
    ) {
        main(cls := "box page-small search")(
          h1(trans.advancedSearch()),
          st.form(
            rel := "nofollow",
            cls := "box__pad search__form",
            action := s"${routes.Search.index()}#results",
            method := "GET"
          )(dataReqs)(
              globalError(form),
              table(
                tr(
                  th(label(trans.players())),
                  td(cls := "usernames")(List("a", "b").map { p =>
                    div(cls := "half")(form3.input(form("players")(p))(tpe := "text"))
                  })
                ),
                colors(hide = true),
                winner(hide = true),
                loser(hide = true),
                rating,
                hasAi,
                aiLevel,
                source,
                perf,
                mode,
                turns,
                duration,
                clockTime,
                clockIncrement,
                status,
                winnerColor,
                date,
                sort,
                analysed,
                tr(
                  th,
                  td(cls := "action")(
                    button(tpe := "submit", cls := "button")(trans.search()),
                    div(cls := "wait")(
                      spinner,
                      "Searching in ", nbGames.localize, " games"
                    )
                  )
                )
              )
            ),
          div(cls := "search__result", id := "results")(
            paginator.map { pager =>
              val permalink = a(cls := "permalink", href := routes.Search.index(), rel := "nofollow")("Permalink")
              if (pager.nbResults > 0) frag(
                div(cls := "search__status box__pad")(
                  strong(pager.nbResults.localize, " games found"), " • ",
                  permalink
                ),
                div(cls := "search__rows")(
                  pagerNext(pager, np => routes.Search.index(np).url),
                  views.html.game.widgets(pager.currentPageResults)
                )
              )
              else div(cls := "search__status box__pad")(strong("No game found"), " • ", permalink)
            }
          )
        )
      }
  }
}
