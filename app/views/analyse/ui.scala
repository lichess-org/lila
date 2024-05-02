package views.analyse

import play.api.libs.json.{ Json, JsObject }
import chess.Color
import chess.format.pgn.PgnStr

import lila.app.UiEnv.{ *, given }

val ui = lila.analyse.ui.AnalyseUi(helpers)(externalEngineEndpoint)

object bits:

  val dataPanel = attr("data-panel")

  def page(title: String)(using Context): Page =
    Page(title).zoom.robots(false).csp(csp)

  def csp(using Context): Update[lila.ui.ContentSecurityPolicy] =
    ui.csp.compose(_.withPeer.withInlineIconFont.withChessDbCn)

  def analyseModule(mode: String, json: JsObject)(using ctx: Context) =
    PageModule("analyse", Json.obj("mode" -> mode, "cfg" -> json))

object embed:

  def lpv(pgn: PgnStr, orientation: Option[Color], getPgn: Boolean)(using ctx: EmbedContext) =
    views.base.embed(
      title = "Lichess PGN viewer",
      cssModule = "lpv.embed",
      modules = EsmInit("site.lpvEmbed")
    )(
      div(cls := "is2d")(div(pgn)),
      ui.embed.lpvJs(orientation, getPgn)(ctx.nonce.some)
    )

  def notFound(using EmbedContext) =
    views.base.embed(
      title = "404 - Game not found",
      cssModule = "lpv.embed"
    ):
      div(cls := "not-found")(h1("Game not found"))
