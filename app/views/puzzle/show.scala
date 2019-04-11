package views.html.puzzle

import play.api.libs.json.JsObject
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(puzzle: lidraughts.puzzle.Puzzle, data: JsObject, pref: JsObject)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.training.txt(),
      moreCss = responsiveCssTag("puzzle"),
      moreJs = frag(
        jsTag("vendor/sparkline.min.js"),
        jsAt(s"compiled/lidraughts.puzzle${isProd ?? (".min")}.js"),
        embedJs(s"""
lidraughts = lidraughts || {};
lidraughts.puzzle = { data: ${safeJsonValue(data)}, pref: ${safeJsonValue(pref)}, i18n: ${bits.jsI18n} };""")
      ),
      draughtsground = false,
      openGraph = lidraughts.app.ui.OpenGraph(
        image = cdnUrl(routes.Export.puzzlePngVariant(puzzle.id, puzzle.variant.key).url).some,
        title = s"${if (puzzle.variant.standard) "Draughts" else puzzle.variant.name} tactic #${puzzle.id} - ${puzzle.color.name.capitalize} to play",
        url = s"$netBaseUrl${routes.Puzzle.showVariant(puzzle.id, puzzle.variant.key).url}",
        description = s"Lidraughts tactic trainer: " + puzzle.color.fold(
          trans.findTheBestMoveForWhite,
          trans.findTheBestMoveForBlack
        ).txt() + s" Played by ${puzzle.attempts} players."
      ).some,
      zoomable = true
    ) {
        main(cls := "puzzle")(
          st.aside(cls := "puzzle__side")(
            div(cls := "puzzle__side__metas")(spinner)
          ),
          div(cls := "puzzle__board main-board")(draughtsgroundSvg),
          div(cls := "puzzle__tools"),
          div(cls := "puzzle__controls"),
          div(cls := "puzzle__history")
        )
      }
}
