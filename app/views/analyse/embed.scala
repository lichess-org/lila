package views.html.analyse

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.EmbedConfig
import lila.app.ui.EmbedConfig.given
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue
import chess.format.pgn.PgnStr

object embed:

  def lpv(pgn: PgnStr, orientation: Option[chess.Color])(using config: EmbedConfig) =
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

  def notFound(using EmbedConfig) =
    views.html.base.embed(
      title = "404 - Game not found",
      cssModule = "analyse.embed"
    )(
      div(cls := "not-found")(
        h1("Game not found")
      )
    )
