package views.html.board

import controllers.routes
import play.api.libs.json.JsObject
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object userAnalysis {

  def apply(data: JsObject, pov: lila.game.Pov, withForecast: Boolean = false)(implicit
      ctx: Context,
  ) =
    views.html.base.layout(
      title = trans.analysis.txt(),
      moreCss = frag(
        cssTag("analyse.free"),
        withForecast option cssTag("analyse.forecast"),
        ctx.blind option cssTag("round.nvui"),
        pov.game.variant.chushogi option chuPieceSprite,
        pov.game.variant.kyotoshogi option kyoPieceSprite,
      ),
      moreJs = frag(
        ctx.blind option analyseNvuiTag,
        moduleJsTag(
          "analyse",
          Json.obj(
            "mode" -> "analyse",
            "data" -> data,
          ),
        ),
      ),
      csp = defaultCsp.withWebAssembly.some,
      shogiground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title = trans.analysis.txt(),
          url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
          description = trans.analysisDescription.txt(),
        )
        .some,
      zoomable = true,
      canonicalPath = lila.common.CanonicalPath(routes.UserAnalysis.index).some,
      withHrefLangs = lila.i18n.LangList.All.some,
    ) {
      main(cls := s"analyse ${mainVariantClass(pov.game.variant)}")(
        pov.game.synthetic option st.aside(cls := "analyse__side")(
          views.html.base.bits.mselect(
            "analyse-variant",
            span(cls := "text", dataIcon := variantIcon(pov.game.variant))(
              span(cls := "inner")(variantName(pov.game.variant)),
            ),
            shogi.variant.Variant.all.map { v =>
              a(
                dataIcon := variantIcon(v),
                cls      := (pov.game.variant == v).option("current"),
                href     := routes.UserAnalysis.parseArg(v.key),
              )(span(cls := "inner")(variantName(v)))
            },
          ),
        ),
        div(cls := s"analyse__board main-board ${variantClass(pov.game.variant)}")(
          shogigroundEmpty(pov.game.variant, pov.color),
        ),
        div(cls := "analyse__tools"),
        div(cls := "analyse__controls"),
      )
    }
}
