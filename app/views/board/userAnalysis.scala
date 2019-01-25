package views.html.board

import play.api.libs.json.JsObject

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.rating.PerfType.iconByVariant

import controllers.routes

object userAnalysis {

  def apply(data: JsObject, pov: lila.game.Pov)(implicit ctx: Context) = views.html.base.layout(
    title = trans.analysis.txt(),
    moreCss = cssTags(List(
      "analyse.css" -> true,
      "forecast.css" -> (!pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined)
    )),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lichess=lichess||{};lichess.user_analysis={data:${safeJsonValue(data)},i18n:${
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
        chess.variant.Variant.all.filterNot(chess.variant.FromPosition ==).map { v =>
          a(dataIcon := iconByVariant(v), href := routes.UserAnalysis.parse(v.key))(v.name)
        }
      )
    ),
    chessground = false,
    openGraph = lila.app.ui.OpenGraph(
      title = "Chess analysis board",
      url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
      description = "Analyse chess positions and variations on an interactive chess board"
    ).some,
    zoomable = true
  ) {
      div(cls := "analyse cg-512")(views.html.board.bits.domPreload(none))
    }
}
