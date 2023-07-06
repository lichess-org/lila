package views.html.opening

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.opening.OpeningPage

object show:

  import bits.*

  def apply(page: OpeningPage, puzzleKey: Option[String])(using ctx: PageContext) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(page.some),
      title = s"${trans.opening.txt()} • ${page.name}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, page.query.uci.lastOption.map(_.uci), None, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = page.name,
          url = s"$netBaseUrl${queryUrl(page.query)}",
          description = page.query.pgnString.value
        )
        .some,
      csp = defaultCsp.withInlineIconFont.withWikiBooks.some
    ) {
      main(cls := "page box box-pad opening")(
        index.searchAndConfig(page.query.config, "", page.query.query.key),
        search.resultsList(Nil),
        h1(cls := "opening__title")(
          page.query.prev match
            case Some(prev) => a(href := queryUrl(prev), title := prev.name, dataIcon := licon.LessThan)
            case None       => a(href := routes.Opening.index(), dataIcon := licon.LessThan)
          ,
          span(cls := "opening__name")(
            page.nameParts.mapWithIndex: (part, i) =>
              frag(
                part match
                  case Left(move) => span(cls := "opening__name__move")(i > 0 option ", ", move)
                  case Right((name, key)) =>
                    val className = s"opening__name__section opening__name__section--${i + 1}"
                    frag(
                      if i == 0 then emptyFrag else if i == 1 then ": " else ", ",
                      key.fold(span(cls := className)(name)) { k =>
                        a(href := keyUrl(k))(cls := className)(name)
                      }
                    )
              ),
            beta
          )
        ),
        div(cls := "opening__intro")(
          div(cls := "opening__intro__result-lpv")(
            div(cls := "opening__intro__result result-bar")(page.explored map { exp =>
              resultSegments(exp.result)
            }),
            div(
              cls              := "lpv lpv--todo lpv--moves-bottom is2d",
              st.data("pgn")   := page.query.pgnString,
              st.data("title") := page.closestOpening.map(_.name)
            )(lpvPreload)
          ),
          div(cls := "opening__intro__content")(
            wiki(page),
            div(cls := "opening__popularity-actions")(
              div(
                cls := "opening__actions"
              )(
                puzzleKey.map { key =>
                  a(cls := "button text", dataIcon := licon.ArcheryTarget, href := routes.Puzzle.show(key))(
                    "Train with puzzles"
                  )
                },
                a(
                  cls      := "button text",
                  dataIcon := licon.Book,
                  href     := s"${routes.UserAnalysis.pgn(page.query.sans mkString "_")}#explorer"
                )(trans.openingExplorer())
              ),
              if page.explored.so(_.history).nonEmpty then
                div(cls := "opening__popularity opening__popularity--chart")(
                  canvas(cls := "opening__popularity__chart")
                )
              else
                p(cls := "opening__popularity opening__error")(
                  "Couldn't fetch the popularity history, try again later."
                )
            )
          )
        ),
        div(cls := "opening__panels")(
          views.html.base.bits.ariaTabList("opening", "next")(
            (
              "next",
              "Popular continuations",
              whatsNext(page) | p(cls := "opening__error")("Couldn't fetch the next moves, try again later.")
            ),
            ("games", "Example games", exampleGames(page))
          )
        )
      )
    }

  private def exampleGames(page: OpeningPage) =
    div(cls := "opening__games")(page.explored.so(_.games).map { game =>
      div(
        cls              := "opening__games__game lpv lpv--todo lpv--moves-bottom is2d",
        st.data("pgn")   := game.pgn.toString,
        st.data("ply")   := page.query.sans.size + 1,
        st.data("title") := titleGame(game.game)
      )(lpvPreload)
    })
