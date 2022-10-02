package views.html.opening

import chess.format.FEN
import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ OpeningConfig, OpeningSearchResult }

object search {

  def form(q: String)(implicit ctx: Context) =
    st.form(cls := "opening__search-form", action := routes.Opening.index, method := "get")(
      input(
        cls            := "opening__search-form__input",
        name           := "q",
        st.placeholder := "Search for openings",
        st.value       := q,
        autofocus      := true,
        autocomplete   := "off",
        spellcheck     := "false"
      ),
      submitButton(cls := "button", dataIcon := "")
    )

  import bits._

  def resultsList(q: String, results: List[OpeningSearchResult])(implicit ctx: Context) =
    div(cls := "opening__search__results")(
      results map { r =>
        a(cls := "opening__search__result", href := routes.Opening.query(r.opening.key))(
          span(cls := "opening__search__result__title")(r.opening.name),
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
        h1("Chess openings"),
        div(cls := "opening__search-config")(
          search.form(q),
          configForm(config, q)
        ),
        resultsList(q, results)
      )
    }
}
