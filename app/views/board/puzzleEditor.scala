package views.html.board

import play.api.libs.json.JsObject

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.rating.PerfType.iconByVariant

import controllers.routes

object puzzleEditor {

  def apply(data: JsObject, pov: lidraughts.game.Pov)(implicit ctx: Context) = views.html.base.layout(
    title = "Puzzle editor",
    moreCss = cssTags(List(
      "analyse.css" -> true
    )),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lidraughts=lidraughts||{};lidraughts.user_analysis={data:${safeJsonValue(data)},i18n:${
        userAnalysisI18n(withForecast = false)
      },explorer:{endpoint:"$explorerEndpoint",tablebaseEndpoint:"$tablebaseEndpoint"}};""")
    ),
    side = pov.game.synthetic option div(cls := "mselect")(
      div(cls := "button", dataIcon := iconByVariant(pov.game.variant))(
        if (pov.game.variant.fromPosition) draughts.variant.Standard.name else pov.game.variant.name,
        iconTag("u")
      ),
      div(cls := "list")(
        lidraughts.pref.Pref.puzzleVariants.map { v =>
          a(dataIcon := iconByVariant(v), href := routes.UserAnalysis.parsePuzzle(v.key))(v.name)
        }
      )
    ),
    draughtsground = false,
    zoomable = true
  ) {
      div(cls := "analyse cg-512")(views.html.board.bits.domPreload(none))
    }
}
