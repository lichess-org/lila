package views.html.opening

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.opening.{ OpeningConfig, OpeningSearchResult }

object search:

  import bits.*

  def form(q: String, focus: Boolean = false) =
    st.form(cls := "opening__search-form", action := routes.Opening.index(), method := "get")(
      input(
        cls            := "opening__search-form__input",
        name           := "q",
        st.placeholder := "Search for openings",
        st.value       := q,
        autofocus      := focus.option("true"),
        autocomplete   := "off",
        spellcheck     := "false"
      ),
      submitButton(cls := "button", dataIcon := licon.Search)
    )

  def resultsList(results: List[OpeningSearchResult]) =
    div(cls := List("opening__search__results" -> true, "none" -> results.isEmpty))(
      results map { r =>
        a(cls := "opening__search__result", href := bits.queryUrl(r.query))(
          span(cls := "opening__search__result__title")(splitName(r.opening)),
          span(cls := "opening__search__result__board")(
            views.html.board.bits.mini(r.opening.fen.board, lastMove = r.opening.lastUci)(span)
          )
        )
      }
    )

  def resultsPage(q: String, results: List[OpeningSearchResult], config: OpeningConfig)(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(none),
      title = s"${trans.opening.txt()} • $q",
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "page box box-pad opening opening--search")(
        index.searchAndConfig(config, q, s"q:$q", searchFocus = true),
        h1(cls := "box__top")("Chess openings"),
        resultsList(results)
      )
    }
