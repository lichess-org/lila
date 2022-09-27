package views.html.opening

import controllers.routes
import play.api.libs.json.{ JsArray, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.common.{ Heapsort, LilaOpening, LilaOpeningFamily }
import lila.opening.OpeningPage
import lila.puzzle.PuzzleOpening
import lila.opening.OpeningQuery

object show {

  def apply(page: OpeningPage, puzzle: Option[PuzzleOpening.FamilyWithCount])(implicit ctx: Context) = {
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = frag(
        jsModule("opening"),
        embedJsUnsafeLoadThen {
          import lila.opening.OpeningHistory.segmentJsonWrite
          s"""LichessOpening.page(${safeJsonValue(
              Json.obj("history" -> JsArray())
            )})"""
        }
      ),
      title = s"${trans.opening.txt()} • $name",
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
        h1(
          a(href := routes.Opening.index, dataIcon := "", cls := "text"),
          page.name
        ),
        div(cls := "opening__intro", page.opening.map(o => style := s"--move-rows: ${(o.pgn.size) + 1}"))(
          div(
            cls              := "lpv lpv--preload lpv--moves-bottom",
            st.data("pgn")   := page.opening.map(_.pgn),
            st.data("title") := page.opening.map(_.name)
          )(lpvPreload),
          div(cls := "opening__intro__side")(
            div(cls := "opening__win-rate")(
              h2(
                "Lichess win rate",
                span(cls := "title-stats")(
                  em("White: ", percentFrag(page.explored.result.whitePercent)),
                  em("Black: ", percentFrag(page.explored.result.blackPercent)),
                  em("Draws: ", percentFrag(page.explored.result.drawsPercent))
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
                href     := s"${routes.UserAnalysis.pgn(page.query.pgn mkString "_")}#explorer"
              )(
                "View in opening explorer"
              )
            ),
            div(cls := "opening__intro__text")("Text here, soon.")
          )
        ),
        div(cls := "opening__nexts")(
          page.explored.next.map { next =>
            a(cls := "opening__next", href := queryUrl(next.query))(
              span(cls := "opening__next__popularity")(
                span(style := s"width:${percentNumber(next.percent)}%")(
                  s"${Math.round(next.percent)}%"
                )
              ),
              span(cls := "opening__next__title")(
                span(cls := "opening__next__san")(next.san)
              ),
              span(cls := "opening__next__board")(
                views.html.board.bits.mini(next.fen, lastMove = next.uci.uci)(span)
              ),
              span(cls := "opening__next__result")(
                resultSegment("white", next.result.whitePercent),
                resultSegment("draws", next.result.drawsPercent),
                resultSegment("black", next.result.blackPercent)
              )
            )
          }
        )
      )
    }
  }

  def queryUrl(q: OpeningQuery) = routes.Opening.query(q.key)

  private val lpvPreload = div(cls := "lpv__board")(div(cls := "cg-wrap")(cgWrapContent))

  private def percentNumber(v: Double) = f"${v}%1.2f"
  private def percentFrag(v: Double)   = frag(strong(percentNumber(v)), "%")

  private def resultSegment(key: String, percent: Double) =
    span(cls := key, style := s"width:${percentNumber(percent)}%")(
      percent > 20 option s"${Math.round(percent)}%"
    )
}
