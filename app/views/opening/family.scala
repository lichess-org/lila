package views.html.opening

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.LilaOpeningFamily
import lila.common.String.html.safeJsonValue
import lila.opening.OpeningFamilyData
import lila.puzzle.PuzzleOpening
import lila.common.Heapsort
import lila.opening.OpeningHistorySegment

object family {

  def apply(
      dataWithAll: OpeningFamilyData.WithAll,
      puzzle: Option[PuzzleOpening.FamilyWithCount]
  )(implicit
      ctx: Context
  ) = {
    import dataWithAll.{ percent => percentHistory, _ }
    import data.fam
    views.html.base.layout(
      moreCss = cssTag("opening"),
      moreJs = frag(
        jsModule("opening"),
        embedJsUnsafeLoadThen {
          import lila.opening.OpeningHistory.segmentJsonWrite
          s"""LichessOpening.family(${safeJsonValue(
              Json.obj("history" -> percentHistory)
            )})"""
        }
      ),
      title = s"${trans.opening.txt()} • ${fam.name}",
      openGraph = lila.app.ui
        .OpenGraph(
          `type` = "article",
          image = cdnUrl(
            s"${routes.Export.fenThumbnail(fam.full.fen.replace(" ", "_"), chess.White.name, fam.full.uci.split(" ").lastOption, none).url}"
          ).some,
          title = fam.name.value,
          url = s"$netBaseUrl${routes.Opening.family(fam.key.value)}",
          description = fam.full.pgn
        )
        .some
    ) {
      main(cls := "page box box-pad opening__family")(
        h1(a(href := routes.Opening.index, dataIcon := "", cls := "text"), fam.name.value),
        div(cls := "opening__intro", style := s"--move-rows: ${(fam.full.uci.count(' ' ==) / 2) + 1}")(
          div(
            cls              := "lpv",
            st.data("pgn")   := fam.full.pgn,
            st.data("title") := fam.full.name
          ),
          div(cls := "opening__intro__side")(
            data.history.lastOption.map { seg =>
              div(cls := "opening__win-rate")(
                h2(
                  "Lichess win rate",
                  span(cls := "title-stats")(
                    em("White: ", percentFrag(seg.whitePercent)),
                    em("Black: ", percentFrag(seg.blackPercent)),
                    em("Draws: ", percentFrag(seg.drawPercent))
                  )
                )
              )
            },
            div(cls := "opening__intro__actions")(
              puzzle.map { p =>
                a(cls := "button text", dataIcon := "", href := routes.Puzzle.show(p.family.key.value))(
                  "Train with puzzles"
                )
              },
              a(
                cls      := "button text",
                dataIcon := "",
                href     := s"${routes.UserAnalysis.pgn(fam.full.pgn.replace(" ", "_"))}#explorer"
              )(
                "View in opening explorer"
              )
            ),
            div(cls := "opening__intro__text")("Text here?")
          )
        ),
        percentHistory.nonEmpty option div(cls := "opening__popularity")(
          h2(
            "Lichess popularity",
            span(cls := "title-stats")(
              em("Average: ", percentFrag(percentHistory.map(_.sum).sum / percentHistory.size)),
              percentHistory.lastOption.map { seg => em("Current: ", percentFrag(seg.sum)) },
              Heapsort
                .topN(percentHistory, 1, Ordering.by[OpeningHistorySegment[Float], Float](_.sum))
                .headOption map { seg => em("Peak: ", percentFrag(seg.sum), " on ", seg.month) }
            )
          ),
          canvas(cls := "opening__popularity__chart")
        )
      )
    }
  }

  private def percentNumber(v: Float) = f"${v}%1.1f"
  private def percentFrag(v: Float)   = frag(strong(percentNumber(v)), "%")
}
