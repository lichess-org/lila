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

  def apply(page: OpeningPage, puzzle: Option[PuzzleOpening.FamilyWithCount])(implicit ctx: Context) = {
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
      main(cls := "page box box-pad opening")(
        h1(
          a(href := routes.Opening.index, dataIcon := "", cls := "text"),
          page.name
        ),
        div(cls := "opening__intro", page.opening.map(o => style := s"--move-rows: ${(o.nbMoves) + 1}"))(
          div(
            cls              := "lpv lpv--preload lpv--moves-bottom",
            st.data("pgn")   := page.opening.map(_.ref.pgn),
            st.data("title") := page.opening.map(_.ref.name)
          )(lpvPreload),
          div(cls := "opening__intro__side")(
            div(cls := "opening__win-rate")(
              h2(
                "Lichess win rate",
                span(cls := "title-stats")(
                  em("White: ", percentFrag(page.explored.result.whitePercent)),
                  em("Black: ", percentFrag(page.explored.result.blackPercent)),
                  em("Draws: ", percentFrag(page.explored.result.drawPercent))
                )
              )
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
                href := s"${page.opening.fold(
                    routes.UserAnalysis.parseArg(page.query.fen.value.replace(" ", "_"))
                  )(o => routes.UserAnalysis.pgn(o.ref.pgn.replace(" ", "_")))}#explorer"
              )(
                "View in opening explorer"
              )
            ),
            div(cls := "opening__intro__text")("Text here, soon.")
          )
        )
      )
    }
  }

  private val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  private def percentNumber(v: Float) = f"${v}%1.2f"
  private def percentFrag(v: Float)   = frag(strong(percentNumber(v)), "%")
}
