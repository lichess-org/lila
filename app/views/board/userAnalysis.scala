package views.html.board

import play.api.libs.json.{ JsObject, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.rating.PerfType.iconByVariant

import controllers.routes

object userAnalysis {

  def apply(data: JsObject, pov: lila.game.Pov, withForecast: Boolean = false)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.analysis.txt(),
      moreCss = frag(
        cssTag("analyse.free"),
        withForecast option cssTag("analyse.forecast"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafe(s"""lishogi=lishogi||{};lishogi.user_analysis=${safeJsonValue(
            Json.obj(
              "data" -> data,
              "i18n" -> userAnalysisI18n(withForecast = withForecast),
              "explorer" -> Json.obj(
                "endpoint"          -> explorerEndpoint,
                "tablebaseEndpoint" -> tablebaseEndpoint
              )
            )
          )}""")
      ),
      csp = defaultCsp.withWebAssembly.some,
      shogiground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Shogi analysis board",
          url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
          description = "Analyse shogi positions and variations on an interactive shogi board"
        )
        .some,
      zoomable = true
    ) {
      main(cls := s"analyse variant-${pov.game.variant.key}")(
        pov.game.synthetic option st.aside(cls := "analyse__side")(
          views.html.base.bits.mselect(
            "analyse-variant",
            span(cls := "text", dataIcon := iconByVariant(pov.game.variant))(pov.game.variant.name),
            shogi.variant.Variant.all.map { v =>
              a(
                dataIcon := iconByVariant(v),
                cls      := (pov.game.variant == v).option("current"),
                href     := routes.UserAnalysis.parseArg(v.key)
              )(v.name)
            }
          )
        ),
        div(cls := "analyse__board main-board")(shogigroundBoard(pov.game.variant, pov.color.some)),
        sgHandTop,
        div(cls := "analyse__tools"),
        sgHandBottom,
        div(cls := "analyse__controls")
      )
    }
}
