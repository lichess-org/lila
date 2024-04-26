package views.opening

import chess.opening.Opening

import lila.app.templating.Environment.{ *, given }
import lila.opening.{ OpeningPage, OpeningSearchResult, OpeningConfig, OpeningTree }

lazy val bits = lila.opening.ui.OpeningBits(helpers)
lazy val wiki = lila.opening.ui.WikiUi(helpers, bits)
lazy val ui   = lila.opening.ui.OpeningUi(helpers, bits, wiki)

def index(page: OpeningPage, wikiMissing: List[Opening])(using ctx: PageContext) =
  views.base.layout(
    moreCss = cssTag("opening"),
    pageModule = bits.pageModule(page.some).some,
    title = trans.site.opening.txt(),
    openGraph = lila.web
      .OpenGraph(
        `type` = "article",
        image = cdnUrl(
          s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, none, none, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
        ).some,
        title = "Chess openings",
        url = s"$netBaseUrl${routes.Opening.index()}",
        description = "Explore the chess openings"
      )
      .some,
    csp = defaultCsp.withInlineIconFont.some
  )(ui.index(page, wikiMissing))

def show(page: OpeningPage, puzzleKey: Option[String])(using ctx: PageContext) =
  views.base.layout(
    moreCss = cssTag("opening"),
    pageModule = bits.pageModule(page.some).some,
    title = s"${trans.site.opening.txt()} • ${page.name}",
    openGraph = lila.web
      .OpenGraph(
        `type` = "article",
        image = cdnUrl(
          s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, page.query.uci.lastOption.map(_.uci), None, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
        ).some,
        title = page.name,
        url = s"$netBaseUrl${bits.queryUrl(page.query)}",
        description = page.query.pgnString.value
      )
      .some,
    csp = defaultCsp.withInlineIconFont.withExternalAnalysisApis.some
  )(ui.show(page, puzzleKey))

def searchResultsPage(q: String, results: List[OpeningSearchResult], config: OpeningConfig)(using
    PageContext
) =
  views.base.layout(
    moreCss = cssTag("opening"),
    pageModule = bits.pageModule(none).some,
    title = s"${trans.site.opening.txt()} • $q",
    csp = defaultCsp.withInlineIconFont.some
  ):
    main(cls := "page box box-pad opening opening--search")(
      ui.searchAndConfig(config, q, s"q:$q", searchFocus = true),
      h1(cls := "box__top")("Chess openings"),
      ui.resultsList(results)
    )

def tree(root: OpeningTree, config: OpeningConfig)(using PageContext) =
  views.base.layout(
    moreCss = cssTag("opening"),
    pageModule = bits.pageModule(none).some,
    title = trans.site.opening.txt()
  )(ui.tree(root, config))
