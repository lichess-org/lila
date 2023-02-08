package views.html.analyse

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue

object embed:

  import EmbedConfig.implicits.*

  def lpv(pgn: String, orientation: Option[chess.Color])(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "Lichess PGN viewer",
      cssModule = "lpv.embed"
    )(
      div(cls := "is2d")(div(pgn)),
      jsModule("lpv.embed"),
      embedJsUnsafe(
        s"""document.addEventListener("DOMContentLoaded",function(){LpvEmbed(document.body.firstChild.firstChild,${safeJsonValue(
            Json
              .obj(
                "i18n" -> i18nJsObject(lpvI18n)
              )
              .add("orientation", orientation.map(_.name))
          )})})""",
        config.nonce
      )
    )

  val lpvI18n = List(
    trans.flipBoard,
    trans.analysis,
    trans.practiceWithComputer,
    trans.download
  )

  def notFound(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "404 - Game not found",
      cssModule = "analyse.embed"
    )(
      div(cls := "not-found")(
        h1("Game not found")
      )
    )
