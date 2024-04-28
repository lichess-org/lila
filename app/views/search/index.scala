package views.search

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }

import scalalib.paginator.Paginator

object index:

  import trans.search.*

  def apply(form: Form[?], paginator: Option[Paginator[Game]] = None, nbGames: Long)(using
      ctx: PageContext
  ) =
    val commons = bits.of(form)
    import commons.*
    views.base.layout(
      title = searchInXGames.txt(nbGames.localize, nbGames),
      modules = EsmInit("bits.gameSearch") ++ infiniteScrollEsmInit,
      moreCss = cssTag("search")
    ) {
      main(cls := "box page-small search")(
        h1(cls := "box__top")(advancedSearch()),
        st.form(
          noFollow,
          cls    := "box__pad search__form",
          action := s"${routes.Search.index()}#results",
          method := "GET"
        )(dataReqs)(
          globalError(form),
          table(
            tr(
              th(label(trans.site.players())),
              td(cls := "usernames two-columns")(List("a", "b").map { p =>
                div(form3.input(form("players")(p))(tpe := "text"))
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
                submitButton(cls := "button")(trans.search.search()),
                div(cls := "wait")(
                  spinner,
                  searchInXGames(nbGames.localize)
                )
              )
            )
          )
        ),
        div(cls := "search__result", id := "results")(
          paginator.map { pager =>
            val permalink =
              a(cls := "permalink", href := routes.Search.index(), noFollow)("Permalink")
            if pager.nbResults > 0 then
              frag(
                div(cls := "search__status box__pad")(
                  strong(xGamesFound(pager.nbResults.localize, pager.nbResults)),
                  " • ",
                  permalink
                ),
                div(cls := "search__rows infinite-scroll")(
                  views.game.widgets(pager.currentPageResults),
                  pagerNext(pager, np => routes.Search.index(np).url)
                )
              )
            else
              div(cls := "search__status box__pad")(
                strong(xGamesFound(0)),
                " • ",
                permalink
              )
          }
        )
      )
    }
