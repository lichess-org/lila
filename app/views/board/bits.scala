package views.board

import chess.format.{ BoardFen, Fen, Uci }
import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.ui.Context

object bits:

  private def miniOrientation(pov: Pov): chess.Color =
    if pov.game.variant == chess.variant.RacingKings then chess.White else pov.player.color

  def mini(pov: Pov)(using ctx: Context): Tag => Tag =
    chessgroundMini(
      if ctx.me.flatMap(pov.game.player).exists(_.blindfold) && pov.game.playable
      then BoardFen("8/8/8/8/8/8/8/8")
      else Fen.writeBoard(pov.game.board),
      miniOrientation(pov),
      pov.game.history.lastMove
    )

  def miniSpan(fen: BoardFen, color: chess.Color = chess.White, lastMove: Option[Uci] = None) =
    chessgroundMini(fen, color, lastMove)(span)

  private def explorerConfig(using ctx: Context) = Json.obj(
    "endpoint"          -> explorerEndpoint,
    "tablebaseEndpoint" -> tablebaseEndpoint,
    "showRatings"       -> ctx.pref.showRatings
  )
  def explorerAndCevalConfig(using Context) =
    Json.obj(
      "explorer"               -> explorerConfig,
      "externalEngineEndpoint" -> externalEngineEndpoint
    )
