package views.board

import chess.variant.{ Crazyhouse, FromPosition, Variant }

import play.api.libs.json.{ JsObject, Json }

import lila.app.UiEnv.{ *, given }

import lila.rating.PerfType.iconByVariant

object userAnalysis:

  def apply(
      data: JsObject,
      pov: Pov,
      withForecast: Boolean = false,
      inlinePgn: Option[String] = None
  )(using ctx: Context) =
    Page(trans.site.analysis.txt())
      .cssTag("analyse.free")
      .cssTag((pov.game.variant == Crazyhouse).option("analyse.zh"))
      .cssTag(withForecast.option("analyse.forecast"))
      .cssTag(ctx.blind.option("round.nvui"))
      .js(analyseNvuiTag)
      .js(
        views.analyse.bits
          .analyseModule(
            "userAnalysis",
            Json
              .obj(
                "data" -> data,
                "i18n" -> views.userAnalysisI18n(withForecast = withForecast),
                "wiki" -> pov.game.variant.standard
              )
              .add("inlinePgn", inlinePgn) ++
              views.board.bits.explorerAndCevalConfig
          )
      )
      .csp(views.analyse.ui.csp.compose(_.withExternalAnalysisApis))
      .graph(
        title = "Chess analysis board",
        url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
        description = "Analyse chess positions and variations on an interactive chess board"
      )
      .zoom:
        main(
          cls := List(
            "analyse"       -> true,
            "analyse--wiki" -> pov.game.variant.standard
          )
        )(
          pov.game.synthetic.option(
            st.aside(cls := "analyse__side")(
              lila.ui.bits.mselect(
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
              pov.game.variant.standard.option(div(cls := "analyse__wiki"))
            )
          ),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools"),
          div(cls := "analyse__controls")
        )
