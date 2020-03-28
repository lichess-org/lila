package views.html.board

import play.api.libs.json.JsObject

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.rating.PerfType.iconByVariant

import controllers.routes

object userAnalysis {

  def apply(data: JsObject, pov: lidraughts.game.Pov)(implicit ctx: Context) = views.html.base.layout(
    title = trans.analysis.txt(),
    moreCss = cssTags(List(
      "analyse.css" -> true,
      "forecast.css" -> (!pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined)
    )),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lidraughts=lidraughts||{};lidraughts.user_analysis={data:${safeJsonValue(data)},i18n:${
        userAnalysisI18n(
          withForecast = !pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined
        )
      },explorer:{endpoint:"$explorerEndpoint",tablebaseEndpoint:"$tablebaseEndpoint"}};""")
    ),
    side = pov.game.synthetic option div(cls := "mselect")(
      div(cls := "button", dataIcon := iconByVariant(pov.game.variant))(
        pov.game.variant.name,
        iconTag("u")
      ),
      div(cls := "list")(
        draughts.variant.Variant.all.filterNot(draughts.variant.FromPosition ==).map { v =>
          a(dataIcon := iconByVariant(v), href := routes.UserAnalysis.parse(v.key))(v.name)
        }
      )
    ),
    draughtsground = false,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Draughts analysis board",
      url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
      description = "Analyse draughts positions and variations on an interactive draughts board"
    ).some,
    zoomable = true
  ) {
      div(cls := "analyse cg-512")(views.html.board.bits.domPreload(none))
    }
}
