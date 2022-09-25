package views.html.opening

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.common.{ Heapsort, LilaOpening, LilaOpeningFamily }
import lila.opening.OpeningPage
import lila.puzzle.PuzzleOpening

object show {

  def apply(page: OpeningPage)(implicit ctx: Context) = {
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = frag(
        jsModule("opening"),
        embedJsUnsafeLoadThen {
          import lila.opening.OpeningHistory.segmentJsonWrite
          s"""LichessOpening.family(${safeJsonValue(
              Json.obj("history" -> null)
            )})"""
        }
      ),
      title = s"${trans.opening.txt()} • $name",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(page.query.fen.value, chess.White.name, page.opening.flatMap(_.ref.uci.split(" ").lastOption), none, ctx.pref.theme.some, ctx.pref.pieceSet.some).url}"
          ).some,
          title = page.name,
          url = s"$netBaseUrl${routes.Opening.query(page.key)}",
          description = page.opening.??(_.ref.pgn)
        )
        .some,
      csp = defaultCsp.withInlineIconFont.some
    ) {
      main(cls := "page box box-pad opening__family")(
      )
    }
  }

  private val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  private def percentNumber(v: Float) = f"${v}%1.2f"
  private def percentFrag(v: Float)   = frag(strong(percentNumber(v)), "%")
}
