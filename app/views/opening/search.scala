package views.html.opening

import chess.format.FEN
import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ OpeningConfig, OpeningSearchResult }

object search {

  import bits._

  def form(q: String, focus: Boolean = false)(implicit ctx: Context) =
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
      submitButton(cls := "button", dataIcon := "")
    )

  def resultsList(results: List[OpeningSearchResult])(implicit ctx: Context) =
    div(cls := List("opening__search__results" -> true, "none" -> results.isEmpty))(
      results map { r =>
        a(cls := "opening__search__result", href := routes.Opening.query(r.opening.key))(
          span(cls := "opening__search__result__title")(splitName(r.opening)),
          span(cls := "opening__search__result__board")(
            views.html.board.bits.mini(FEN(r.opening.fen), lastMove = ~r.opening.lastUci)(span)
          )
        )
      }
    )

  def resultsPage(q: String, results: List[OpeningSearchResult], config: OpeningConfig)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(none),
      title = s"${trans.opening.txt()} • $q",
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "page box box-pad opening opening--search")(
        index.searchAndConfig(config, q, s"q:$q", searchFocus = true),
        h1("Chess openings"),
        resultsList(results)
      )
    }
}
