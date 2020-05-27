package views.html.board

import play.api.libs.json.{ Json, JsObject }

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.rating.PerfType.iconByVariant

import controllers.routes

object userAnalysis {

  def apply(data: JsObject, pov: lidraughts.game.Pov)(implicit ctx: Context) = views.html.base.layout(
    title = trans.analysis.txt(),
    moreCss = frag(
      cssTag("analyse.free"),
      !pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined option cssTag("analyse.forecast"),
      ctx.blind option cssTag("round.nvui")
    ),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJsUnsafe(s"""lidraughts=lidraughts||{};lidraughts.user_analysis=${
        safeJsonValue(Json.obj(
          "data" -> data,
          "i18n" -> userAnalysisI18n(
            withForecast = !pov.game.synthetic && pov.game.playable && ctx.me.flatMap(pov.game.player).isDefined
          ),
          "explorer" -> Json.obj(
            "endpoint" -> explorerEndpoint,
            "tablebaseEndpoint" -> tablebaseEndpoint
          )
        ))
      }""")
    ),
    draughtsground = false,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = "Draughts analysis board",
      url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
      description = "Analyse draughts positions and variations on an interactive draughts board"
    ).some,
    zoomable = true
  ) {
      main(cls := "analyse")(
        pov.game.synthetic option st.aside(cls := "analyse__side")(
          views.html.base.bits.mselect(
            "analyse-variant",
            span(cls := "text", dataIcon := iconByVariant(pov.game.variant))(pov.game.variant.name),
            draughts.variant.Variant.allVariants.map { v =>
              a(
                dataIcon := iconByVariant(v),
                cls := (pov.game.variant == v).option("current"),
                href := routes.UserAnalysis.parse(v.key)
              )(v.name)
            }
          )
        ),
        div(cls := "analyse__board main-board")(draughtsgroundBoard),
        div(cls := "analyse__tools"),
        div(cls := "analyse__controls")
      )
    }
}
