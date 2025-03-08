package lila.analyse
package ui

import chess.variant.*
import chess.format.{ Uci, Fen }
import play.api.libs.json.*

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.{ *, given }

final class AnalyseUi(helpers: Helpers)(endpoints: AnalyseEndpoints):
  import helpers.{ *, given }

  def miniSpan(fen: Fen.Board, color: Color = chess.White, lastMove: Option[Uci] = None) =
    chessgroundMini(fen, color, lastMove)(span)

  def explorerAndCevalConfig(using ctx: Context) =
    Json.obj(
      "explorer" -> Json.obj(
        "endpoint"          -> endpoints.explorer,
        "tablebaseEndpoint" -> endpoints.tablebase,
        "showRatings"       -> ctx.pref.showRatings
      ),
      "externalEngineEndpoint" -> endpoints.externalEngine
    )

  def userAnalysis(
      data: JsObject,
      pov: Pov,
      withForecast: Boolean = false,
      inlinePgn: Option[String] = None
  )(using ctx: Context): Page =
    Page(trans.site.analysis.txt())
      .css("analyse.free")
      .css((pov.game.variant == Crazyhouse).option("analyse.zh"))
      .css(withForecast.option("analyse.forecast"))
      .css(ctx.blind.option("round.nvui"))
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .csp(bits.cspExternalEngine.compose(_.withExternalAnalysisApis))
      .js(analyseNvuiTag)
      .js:
        bits.analyseModule(
          "userAnalysis",
          Json
            .obj(
              "data" -> data,
              "wiki" -> pov.game.variant.standard
            )
            .add("inlinePgn", inlinePgn) ++
            explorerAndCevalConfig
        )
      .i18n(_.puzzle, _.study)
      .i18nOpt(ctx.blind, _.keyboardMove)
      .graph(
        title = "Chess analysis board",
        url = s"$netBaseUrl${routes.UserAnalysis.index.url}",
        description = "Analyse chess positions and variations on an interactive chess board"
      )
      .flag(_.zoom):
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
                    href     := routes.UserAnalysis.parseArg(v.key.value)
                  )(v.name)
                }
              ),
              pov.game.variant.standard.option(
                fieldset(cls := "analyse__wiki empty toggle-box toggle-box--toggle", id := "wikibook-field")(
                  legend(tabindex := 0)("WikiBook"),
                  div(cls := "analyse__wiki-text")
                )
              )
            )
          ),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools"),
          div(cls := "analyse__controls")
        )

  private def iconByVariant(variant: Variant): Icon =
    PerfKey.byVariant(variant).fold(Icon.CrownElite)(_.perfIcon)

  def titleOf(pov: Pov)(using Translate) =
    val opening = pov.game.opening.fold(trans.site.analysis.txt())(_.opening.name)
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: $opening"

  object bits:

    val dataPanel = attr("data-panel")

    def page(title: String)(using Context): Page =
      Page(title)
        .flag(_.zoom)
        .flag(_.noRobots)
        .csp:
          cspExternalEngine.compose(_.withPeer.withInlineIconFont.withChessDbCn)

    def cspExternalEngine: Update[ContentSecurityPolicy] =
      _.withWebAssembly.withExternalEngine(endpoints.externalEngine)

    def analyseModule(mode: String, json: JsObject) =
      PageModule("analyse", Json.obj("mode" -> mode, "cfg" -> json))

  object embed:

    def lpvJs(orientation: Option[Color], getPgn: Boolean)(using Translate): WithNonce[Frag] =
      lpvJs(lpvConfig(orientation, getPgn))

    def lpvJs(lpvConfig: JsObject)(using Translate): WithNonce[Frag] =
      embedJsUnsafe(s"""document.addEventListener("DOMContentLoaded",function(){LpvEmbed(${safeJsonValue(
          lpvConfig + ("i18n" -> Json.obj(
            "flipTheBoard"         -> trans.site.flipBoard.txt(),
            "analysisBoard"        -> trans.site.analysis.txt(),
            "practiceWithComputer" -> trans.site.practiceWithComputer.txt(),
            "getPgn"               -> trans.study.copyChapterPgn.txt(),
            "download"             -> trans.site.download.txt()
          ))
        )})})""")

    def lpvConfig(orientation: Option[Color], getPgn: Boolean) = Json
      .obj(
        "menu" -> Json.obj(
          "getPgn" -> Json.obj("enabled" -> getPgn)
        )
      )
      .add("orientation", orientation.map(_.name))
