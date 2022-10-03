package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ OpeningPage, OpeningQuery }
import lila.puzzle.PuzzleOpening
import lila.opening.NamePart

object show {

  import bits._

  def apply(page: OpeningPage, puzzle: Option[PuzzleOpening.FamilyWithCount])(implicit ctx: Context) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(page.some),
      title = s"${trans.opening.txt()} • ${page.name}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, page.opening.flatMap(_.uci.split(" ").lastOption), none, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = page.name,
          url = s"$netBaseUrl${queryUrl(page.query)}",
          description = page.opening.??(_.pgn)
        )
        .some,
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "page box box-pad opening")(
        index.searchAndConfig(page.query.config, "", page.query.key),
        search.resultsList(Nil),
        h1(cls := "opening__title")(
          page.query.prev match {
            case Some(prev) => a(href := queryUrl(prev), title := prev.name, dataIcon := "", cls := "text")
            case None       => a(href := routes.Opening.index(), dataIcon := "", cls := "text")
          },
          span(cls := "opening__name")(
            page.nameParts.zipWithIndex map { case (NamePart(name, key), i) =>
              frag(
                if (i == 1) br else if (i > 1) ", " else emptyFrag,
                key.fold(span(name))(q => a(href := routes.Opening.query(q))(name))(
                  cls := s"opening__name__section opening__name__section--${i + 1}"
                )
              )
            }
          )
        ),
        div(cls := "opening__intro")(
          div(cls := "opening__intro__result-lpv")(
            div(cls := "opening__intro__result result-bar")(resultSegments(page.explored.result)),
            div(
              cls              := "lpv lpv--todo lpv--moves-bottom",
              st.data("pgn")   := page.query.pgnString,
              st.data("title") := page.opening.map(_.name)
            )(lpvPreload)
          ),
          div(cls := "opening__intro__content")(
            div(cls := "opening__intro__text")("No description of the opening, yet."),
            div(cls      := "opening__popularity")(
              canvas(cls := "opening__popularity__chart")
            ),
            div(cls := "opening__intro__actions")(
              puzzle.map { p =>
                a(cls := "button text", dataIcon := "", href := routes.Puzzle.show(p.family.key.value))(
                  "Train with puzzles"
                )
              },
              a(
                cls      := "button text",
                dataIcon := "",
                href     := s"${routes.UserAnalysis.pgn(page.query.pgn mkString "_")}#explorer"
              )(
                "View in opening explorer"
              )
            )
          )
        ),
        div(cls := "opening__panels")(
          views.html.base.bits.ariaTabList("opening", "next")(
            ("next", "Popular continuations", whatsNext(page)),
            ("games", "Example games", exampleGames(page))
          )
        )
      )
    }

  private def exampleGames(page: OpeningPage)(implicit ctx: Context) =
    div(cls := "opening__games")(page.explored.games.map { game =>
      div(
        cls              := "opening__games__game lpv lpv--todo lpv--moves-bottom",
        st.data("pgn")   := game.pgn.toString,
        st.data("ply")   := page.query.pgn.size + 1,
        st.data("title") := titleGame(game.game)
      )(lpvPreload)
    })
}
