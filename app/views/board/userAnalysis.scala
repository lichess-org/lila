package views.html.board

import play.api.libs.json.{ JsObject, Json }

import chess.variant.{ Variant, FromPosition, Crazyhouse }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.rating.PerfType.iconByVariant

import controllers.routes

object userAnalysis:

  def apply(
      data: JsObject,
      pov: lila.game.Pov,
      withForecast: Boolean = false,
      inlinePgn: Option[String] = None
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = trans.analysis.txt(),
      moreCss = frag(
        cssTag("analyse.free"),
        pov.game.variant == Crazyhouse option cssTag("analyse.zh"),
        withForecast option cssTag("analyse.forecast"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        analyseNvuiTag,
        analyseInit(
          "userAnalysis",
          Json
            .obj(
              "data" -> data,
              "i18n" -> userAnalysisI18n(withForecast = withForecast),
              "wiki" -> pov.game.variant.standard
            )
            .add("inlinePgn", inlinePgn) ++
            views.html.board.bits.explorerAndCevalConfig
        )
      ),
      csp = analysisCsp.withWikiBooks.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Chess analysis board",
          url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
          description = "Analyse chess positions and variations on an interactive chess board"
        )
        .some,
      zoomable = true
    ) {
      main(
        cls := List(
          "analyse"       -> true,
          "analyse--wiki" -> pov.game.variant.standard
        )
      )(
        pov.game.synthetic option st.aside(cls := "analyse__side")(
          views.html.base.bits.mselect(
            "analyse-variant",
            span(cls := "text", dataIcon := iconByVariant(pov.game.variant))(pov.game.variant.name),
            Variant.list.all.filter(FromPosition != _).map { v =>
              a(
                dataIcon := iconByVariant(v),
                cls      := (pov.game.variant == v).option("current"),
                href     := routes.UserAnalysis.parseArg(v.key)
              )(v.name)
            }
          ),
          pov.game.variant.standard option div(cls := "analyse__wiki")
        ),
        div(cls := "analyse__board main-board")(chessgroundBoard),
        div(cls := "analyse__tools"),
        div(cls := "analyse__controls")
      )
    }
