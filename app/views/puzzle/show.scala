package views.html.puzzle

import play.api.libs.json.{ JsObject, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(puzzle: lila.puzzle.Puzzle, data: JsObject, pref: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.puzzles.txt(),
      moreCss = cssTag("puzzle"),
      moreJs = frag(
        jsTag("vendor/sparkline.min.js"),
        jsModule("puzzle"),
        embedJsUnsafeLoadThen(s"""LichessPuzzle(${safeJsonValue(
          Json.obj(
            "data" -> data,
            "pref" -> pref,
            "i18n" -> bits.jsI18n()
          )
        )})""")
      ),
      csp = defaultCsp.withWebAssembly.some,
      chessground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          image = cdnUrl(routes.Export.puzzleThumbnail(puzzle.id).url).some,
          title = s"Chess tactic #${puzzle.id} - ${puzzle.color.name.capitalize} to play",
          url = s"$netBaseUrl${routes.Puzzle.show(puzzle.id).url}",
          description = s"Lichess tactic trainer: " + puzzle.color
            .fold(
              trans.findTheBestMoveForWhite,
              trans.findTheBestMoveForBlack
            )
            .txt() + s" Played by ${puzzle.attempts} players."
        )
        .some,
      zoomable = true
    ) {
      main(cls := "puzzle")(
        st.aside(cls := "puzzle__side")(
          div(cls := "puzzle__side__metas")(spinner)
        ),
        div(cls := "puzzle__board main-board")(chessgroundBoard),
        div(cls := "puzzle__tools"),
        div(cls := "puzzle__controls"),
        div(cls := "puzzle__history")
      )
    }
}
