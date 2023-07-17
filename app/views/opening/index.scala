package views.html.opening

import chess.opening.Opening
import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.opening.{ OpeningConfig, OpeningPage }

object index:

  import bits.*

  def apply(page: OpeningPage, wikiMissing: List[Opening])(using ctx: PageContext) =
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = moreJs(page.some),
      title = trans.opening.txt(),
      openGraph = lila.app.ui
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
    ) {
      main(cls := "page box box-pad opening opening--index")(
        searchAndConfig(page.query.config, "", ""),
        search.resultsList(Nil),
        boxTop(
          h1("Chess openings", beta),
          div(cls := "box__top__actions")(
            a(href := routes.Opening.tree)("Name tree"),
            a(href := s"${routes.UserAnalysis.index}#explorer")("Explorer")
          )
        ),
        whatsNext(page) | p(cls := "opening__error")("Couldn't fetch the next moves, try again later."),
        isGranted(_.OpeningWiki) option wiki.showMissing(wikiMissing)
      )
    }

  def searchAndConfig(config: OpeningConfig, q: String, thenTo: String, searchFocus: Boolean = false)(using
      PageContext
  ) =
    div(cls := "opening__search-config")(
      search.form(q, searchFocus),
      configForm(config, thenTo)
    )
