package lila.opening
package ui
import chess.opening.Opening

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class OpeningUi(helpers: Helpers, bits: OpeningBits, wiki: WikiUi):
  import helpers.{ *, given }
  import bits.*

  def index(page: OpeningPage, wikiMissing: List[Opening])(using ctx: Context) =
    Page(trans.site.opening.txt())
      .css("opening")
      .js(bits.pageModule(page.some))
      .graph(
        OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, none, none, none, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = "Chess openings",
          url = s"$netBaseUrl${routes.Opening.index()}",
          description = "Explore the chess openings"
        )
      )
      .csp(_.withInlineIconFont):
        main(cls := "page box box-pad opening opening--index")(
          searchAndConfig(page.query.config, "", ""),
          resultsList(Nil),
          boxTop(
            h1("Chess openings"),
            div(cls := "box__top__actions")(
              a(href := routes.Opening.tree)("Name tree"),
              a(href := s"${routes.UserAnalysis.index}#explorer")("Explorer")
            )
          ),
          whatsNext(page) | p(cls := "opening__error")("Couldn't fetch the next moves, try again later."),
          Granter.opt(_.OpeningWiki).option(showMissing(wikiMissing))
        )

  def tree(root: OpeningTree, config: OpeningConfig)(using Context) =
    Page(trans.site.opening.txt())
      .css("opening")
      .js(bits.pageModule(none)):
        main(cls := "page box box-pad opening opening--tree")(
          searchAndConfig(config, "", "tree"),
          resultsList(Nil),
          boxTop(
            h1("Chess openings name tree"),
            div(cls := "box__top__actions")(
              a(href := routes.Opening.index())("Opening pages"),
              a(href := s"${routes.UserAnalysis.index}#explorer")("Explorer")
            )
          ),
          div(cls := "opening__tree")(
            renderChildren(root, 1)
          )
        )

  def show(page: OpeningPage, puzzleKey: Option[String])(using ctx: Context) =
    Page(s"${trans.site.opening.txt()} • ${page.name}")
      .css("opening")
      .js(bits.pageModule(page.some))
      .graph(
        OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, none, page.query.uci.lastOption, None, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = page.name,
          url = s"$netBaseUrl${bits.queryUrl(page.query)}",
          description = page.query.pgnString.value
        )
      )
      .csp(_.withInlineIconFont.withExternalAnalysisApis):
        main(cls := "page box box-pad opening")(
          searchAndConfig(page.query.config, "", page.query.query.key),
          resultsList(Nil),
          h1(cls := "opening__title")(
            page.query.prev match
              case Some(prev) => a(href := queryUrl(prev), title := prev.name, dataIcon := Icon.LessThan)
              case None => a(href := routes.Opening.index(), dataIcon := Icon.LessThan)
            ,
            span(cls := "opening__name")(
              page.nameParts.mapWithIndex: (part, i) =>
                frag(
                  part match
                    case Left(move) => span(cls := "opening__name__move")((i > 0).option(", "), move)
                    case Right((name, key)) =>
                      val className = s"opening__name__section opening__name__section--${i + 1}"
                      frag(
                        if i == 0 then emptyFrag else if i == 1 then ": " else ", ",
                        key.fold(span(cls := className)(name)) { k =>
                          a(href := openingKeyUrl(k))(cls := className)(name)
                        }
                      )
                )
            )
          ),
          div(cls := "opening__intro")(
            div(cls := "opening__intro__result-lpv")(
              div(cls := "opening__intro__result result-bar")(page.exploredOption.map { exp =>
                resultSegments(exp.result)
              }),
              div(
                cls := "lpv lpv--todo lpv--moves-bottom is2d",
                st.data("pgn") := page.query.pgnString,
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
                    a(cls := "button text", dataIcon := Icon.ArcheryTarget, href := routes.Puzzle.show(key))(
                      "Train with puzzles"
                    )
                  },
                  a(
                    cls := "button text",
                    dataIcon := Icon.Book,
                    href := s"${routes.UserAnalysis.pgn(page.query.sans.mkString("_"))}#explorer"
                  )(trans.site.openingExplorer())
                ),
                page.explored.fold(
                  _ =>
                    p(cls := "opening__popularity opening__error")(
                      "Couldn't fetch the popularity history, try again later."
                    ),
                  exp =>
                    exp
                      .so(_.history)
                      .nonEmpty
                      .option:
                        div(cls := "opening__popularity opening__popularity--chart")(
                          canvas(cls := "opening__popularity__chart")
                        )
                )
              )
            )
          ),
          div(cls := "opening__panels")(
            lila.ui.bits.ariaTabList("opening", "next")(
              (
                "next",
                "Popular continuations",
                whatsNext(page) | p(cls := "opening__error")(
                  "Couldn't fetch the next moves, try again later."
                )
              ),
              ("games", "Example games", exampleGames(page))
            )
          )
        )

  def resultsPage(q: String, results: List[OpeningSearchResult], config: OpeningConfig)(using Context) =
    Page(s"${trans.site.opening.txt()} • $q")
      .css("opening")
      .js(bits.pageModule(none))
      .csp(_.withInlineIconFont):
        main(cls := "page box box-pad opening opening--search")(
          searchAndConfig(config, q, s"q:$q", searchFocus = true),
          h1(cls := "box__top")("Chess openings"),
          resultsList(results)
        )

  private def searchForm(q: String, focus: Boolean) =
    st.form(cls := "opening__search-form", action := routes.Opening.index(), method := "get")(
      input(
        cls := "opening__search-form__input",
        name := "q",
        st.placeholder := "Search for openings",
        st.value := q,
        autofocus := focus.option("true"),
        autocomplete := "off",
        spellcheck := "false"
      ),
      submitButton(cls := "button", dataIcon := Icon.Search)
    )

  def resultsList(results: List[OpeningSearchResult]) =
    div(cls := List("opening__search__results" -> true, "none" -> results.isEmpty))(
      results.map { r =>
        a(cls := "opening__search__result", href := bits.queryUrl(r.query))(
          span(cls := "opening__search__result__title")(splitName(r.opening)),
          span(cls := "opening__search__result__board")(
            chessgroundMini(r.opening.fen.board, lastMove = r.opening.lastUci)(span)
          )
        )
      }
    )

  private def searchAndConfig(config: OpeningConfig, q: String, thenTo: String, searchFocus: Boolean = false)(
      using Context
  ) =
    div(cls := "opening__search-config")(
      searchForm(q, searchFocus),
      configForm(config, thenTo)
    )

  private def renderChildren(node: OpeningTree, level: Int): Frag =
    node.children.map { case (op, node) =>
      val fold = level < 4 && node.children.nonEmpty
      val content = frag(
        (if fold then summary else div) (op match
          case (name, None) => name
          case (name, Some(op)) => a(href := openingUrl(op))(name)),
        renderChildren(node, level + 1)
      )
      if fold then details(content) else content
    }

  private def exampleGames(page: OpeningPage) =
    div(cls := "opening__games")(page.exploredOption.so(_.games).map { game =>
      div(
        cls := "opening__games__game lpv lpv--todo lpv--moves-bottom is2d",
        st.data("pgn") := game.pgn.toString,
        st.data("ply") := page.query.sans.size + 1,
        st.data("title") := titleGame(game.game)
      )(lpvPreload)
    })
