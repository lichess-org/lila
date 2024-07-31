package lila.analyse
package ui

import chess.variant.*
import play.api.libs.json.*

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class AnalyseUi(helpers: Helpers)(externalEngineEndpoint: String):
  import helpers.{ *, given }

  def userAnalysis(
      data: JsObject,
      pov: Pov,
      withForecast: Boolean = false
  )(using ctx: Context) =
    Page(trans.site.analysis.txt())
      .css("analyse.free")
      .css((pov.game.variant == Crazyhouse).option("analyse.zh"))
      .css(withForecast.option("analyse.forecast"))
      .css(ctx.blind.option("round.nvui"))
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .csp(csp.compose(_.withExternalAnalysisApis))
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

  def csp: Update[ContentSecurityPolicy] =
    _.withWebAssembly.withExternalEngine(externalEngineEndpoint)

  def titleOf(pov: Pov)(using Translate) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${pov.game.opening
        .fold(trans.site.analysis.txt())(_.opening.name)}"

  object embed:

    def lpvJs(orientation: Option[Color], getPgn: Boolean)(using Translate): WithNonce[Frag] =
      lpvJs(lpvConfig(orientation, getPgn))

    def lpvJs(lpvConfig: JsObject)(using Translate): WithNonce[Frag] =
      embedJsUnsafe(s"""document.addEventListener("DOMContentLoaded",function(){LpvEmbed(${safeJsonValue(
          lpvConfig ++ Json.obj(
            "i18n" -> i18nJsObject(lpvI18n)
          )
        )})})""")

    def lpvConfig(orientation: Option[Color], getPgn: Boolean) = Json
      .obj(
        "menu" -> Json.obj(
          "getPgn" -> Json.obj("enabled" -> getPgn)
        )
      )
      .add("orientation", orientation.map(_.name))

    private val lpvI18n = List(
      trans.site.flipBoard,
      trans.site.analysis,
      trans.site.practiceWithComputer,
      trans.site.download
    )
