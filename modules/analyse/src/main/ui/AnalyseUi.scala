package lila.analyse
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import play.api.libs.json.JsObject
import play.api.libs.json.Json

final class AnalyseUi(helpers: Helpers)(externalEngineEndpoint: String):
  import helpers.{ *, given }

  def csp: Update[ContentSecurityPolicy] =
    _.withWebAssembly.withExternalEngine(externalEngineEndpoint)

  def titleOf(pov: Pov)(using Translate) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${pov.game.opening
        .fold(trans.site.analysis.txt())(_.opening.name)}"

  object embed:

    def lpvJs(orientation: Option[chess.Color], getPgn: Boolean)(using Translate): WithNonce[Frag] =
      lpvJs(lpvConfig(orientation, getPgn))

    def lpvJs(lpvConfig: JsObject)(using Translate): WithNonce[Frag] =
      embedJsUnsafe(s"""document.addEventListener("DOMContentLoaded",function(){LpvEmbed(${safeJsonValue(
          lpvConfig ++ Json.obj(
            "i18n" -> i18nJsObject(lpvI18n)
          )
        )})})""")

    def lpvConfig(orientation: Option[chess.Color], getPgn: Boolean) = Json
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
