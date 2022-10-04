package views.html.opening

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.opening.{ OpeningPage, OpeningQuery, OpeningWiki }
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
            div(cls := "opening__intro__result result-bar")(page.explored map { exp =>
              resultSegments(exp.result)
            }),
            div(
              cls              := "lpv lpv--todo lpv--moves-bottom",
              st.data("pgn")   := page.query.pgnString,
              st.data("title") := page.opening.map(_.name)
            )(lpvPreload)
          ),
          div(cls := "opening__intro__content")(
            div(cls := "opening__wiki")(
              page.wiki
                .flatMap(_.markup)
                .fold(frag("No description of the opening, yet.")) { markup =>
                  div(cls := "opening__wiki__markup")(raw(markup))
                },
              page.query.opening.ifTrue(isGranted(_.OpeningWiki)) map { op =>
                details(cls := "opening__wiki__editor")(
                  summary(cls := "opening__wiki__editor__summary")("Edit the description"),
                  postForm(action := routes.Opening.wikiWrite(op.key))(
                    form3.textarea(
                      OpeningWiki.form
                        .fill(~page.wiki.flatMap(_.revisions.headOption).map(_.text.value))("text")
                    )(),
                    form3.submit("Save and publish")
                  ),
                  details(cls := "opening__wiki__editor__revisions")(
                    summary("Revision history"),
                    page.wiki.??(_.revisions).map { rev =>
                      div(cls := "opening__wiki__editor__revision")(
                        div(momentFromNowOnce(rev.at), userIdLink(rev.by.some)),
                        textarea(disabled := true)(rev.text)
                      )
                    }
                  )
                )
              }
            ),
            div(cls := "opening__popularity")(
              if (page.explored.??(_.history).nonEmpty)
                canvas(cls := "opening__popularity__chart")
              else p(cls := "opening__error")("Couldn't fetch the popularity history, try again later.")
            ),
            div(
              cls := "opening__intro__actions"
            )(
              puzzle.map { p =>
                a(cls := "button text", dataIcon := "", href := routes.Puzzle.show(p.family.key.value))(
                  "Train with puzzles"
                )
              },
              a(
                cls      := "button text",
                dataIcon := "",
                href     := s"${routes.UserAnalysis.pgn(page.query.pgn mkString "_")}#explorer"
              )("View in opening explorer")
            )
          )
        ),
        div(cls := "opening__panels")(
          views.html.base.bits.ariaTabList("opening", "next")(
            (
              "next",
              "Popular continuations",
              page.explored.map(whatsNext) |
                p(cls := "opening__error")("Couldn't fetch the next moves, try again later.")
            ),
            ("games", "Example games", exampleGames(page))
          )
        )
      )
    }

  private def exampleGames(page: OpeningPage)(implicit ctx: Context) =
    div(cls := "opening__games")(page.explored.??(_.games).map { game =>
      div(
        cls              := "opening__games__game lpv lpv--todo lpv--moves-bottom",
        st.data("pgn")   := game.pgn.toString,
        st.data("ply")   := page.query.pgn.size + 1,
        st.data("title") := titleGame(game.game)
      )(lpvPreload)
    })
}
