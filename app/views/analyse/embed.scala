package views.analyse

import chess.Color
import chess.format.pgn.PgnStr
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ *, given }

import lila.common.String.html.safeJsonValue

object embed:

  def lpv(pgn: PgnStr, orientation: Option[Color], getPgn: Boolean)(using EmbedContext) =
    views.base.embed(
      title = "Lichess PGN viewer",
      cssModule = "lpv.embed",
      modules = EsmInit("site.lpvEmbed")
    )(
      div(cls := "is2d")(div(pgn)),
      lpvJs(orientation, getPgn)
    )

  def lpvJs(orientation: Option[Color], getPgn: Boolean)(using config: EmbedContext): Frag = lpvJs:
    lpvConfig(orientation, getPgn)

  def lpvJs(lpvConfig: JsObject)(using ctx: EmbedContext): Frag =
    embedJsUnsafe(s"""document.addEventListener("DOMContentLoaded",function(){LpvEmbed(${safeJsonValue(
        lpvConfig ++ Json.obj(
          "i18n" -> i18nJsObject(lpvI18n)
        )
      )})})""")(ctx.nonce.some)

  def lpvConfig(orientation: Option[Color], getPgn: Boolean)(using config: EmbedContext) = Json
    .obj(
      "menu" -> Json.obj(
        "getPgn" -> Json.obj("enabled" -> getPgn)
      )
    )
    .add("orientation", orientation.map(_.name))

  val lpvI18n = List(
    trans.site.flipBoard,
    trans.site.analysis,
    trans.site.practiceWithComputer,
    trans.site.download
  )

  def notFound(using EmbedContext) =
    views.base.embed(
      title = "404 - Game not found",
      cssModule = "lpv.embed"
    ):
      div(cls := "not-found"):
        h1("Game not found")
