package views.html.analyse

import play.api.libs.json.{ Json, JsObject }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import chess.Color
import chess.format.pgn.PgnStr

object embed:

  def lpv(pgn: PgnStr, orientation: Option[Color], getPgn: Boolean)(using EmbedContext) =
    views.html.base.embed(
      title = "Lichess PGN viewer",
      cssModule = "lpv.embed"
    )(
      div(cls := "is2d")(div(pgn)),
      jsModule("lpv.embed"),
      lpvJs(orientation, getPgn)
    )

  def lpvJs(orientation: Option[Color], getPgn: Boolean)(using config: EmbedContext): Frag = lpvJs:
    lpvConfig(orientation, getPgn)

  def lpvJs(lpvConfig: JsObject)(using ctx: EmbedContext): Frag = embedJsUnsafe(
    s"""document.addEventListener("DOMContentLoaded",function(){LpvEmbed(${safeJsonValue(
        lpvConfig ++ Json.obj(
          "i18n" -> i18nJsObject(lpvI18n)
        )
      )})})""",
    ctx.nonce
  )

  def lpvConfig(orientation: Option[Color], getPgn: Boolean)(using config: EmbedContext) = Json
    .obj(
      "menu" -> Json.obj(
        "getPgn" -> Json.obj("enabled" -> getPgn)
      )
    )
    .add("orientation", orientation.map(_.name))

  val lpvI18n = List(
    trans.flipBoard,
    trans.analysis,
    trans.practiceWithComputer,
    trans.download
  )

  def notFound(using EmbedContext) =
    views.html.base.embed(
      title = "404 - Game not found",
      cssModule = "lpv.embed"
    ):
      div(cls := "not-found"):
        h1("Game not found")
