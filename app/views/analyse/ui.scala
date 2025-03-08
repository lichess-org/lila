package views.analyse

import chess.format.pgn.PgnStr

import lila.app.UiEnv.{ *, given }

val ui = lila.analyse.ui.AnalyseUi(helpers)(analyseEndpoints)

object embed:

  def lpv(pgn: PgnStr, orientation: Option[Color], getPgn: Boolean)(using ctx: EmbedContext) =
    views.base.embed.minimal(
      title = "Lichess PGN viewer",
      cssKeys = List("bits.lpv.embed"),
      modules = Esm("site.lpvEmbed")
    )(
      div(cls := "is2d")(div(pgn)),
      ui.embed.lpvJs(orientation, getPgn)(ctx.nonce.some)
    )

  def notFound(using EmbedContext) =
    views.base.embed.minimal(
      title = "404 - Game not found",
      cssKeys = List("bits.lpv.embed")
    ):
      div(cls := "not-found")(h1("Game not found"))
