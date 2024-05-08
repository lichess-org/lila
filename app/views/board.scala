package views.board

import chess.format.{ BoardFen, Fen, Uci }
import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }
import lila.ui.Context

def userAnalysis(
    data: JsObject,
    pov: Pov,
    withForecast: Boolean = false,
    inlinePgn: Option[String] = None
)(using Context) =
  views.analyse.ui
    .userAnalysis(data, pov, withForecast)
    .js(analyseNvuiTag)
    .js(
      views.analyse.bits.analyseModule(
        "userAnalysis",
        Json
          .obj(
            "data" -> data,
            "i18n" -> views.userAnalysisI18n(withForecast = withForecast),
            "wiki" -> pov.game.variant.standard
          )
          .add("inlinePgn", inlinePgn) ++
          explorerAndCevalConfig
      )
    )

private def miniOrientation(pov: Pov): Color =
  if pov.game.variant == chess.variant.RacingKings then chess.White else pov.player.color

def mini(pov: Pov)(using ctx: Context): Tag => Tag =
  chessgroundMini(
    if ctx.me.flatMap(pov.game.player).exists(_.blindfold) && pov.game.playable
    then BoardFen("8/8/8/8/8/8/8/8")
    else Fen.writeBoard(pov.game.board),
    miniOrientation(pov),
    pov.game.history.lastMove
  )

def miniSpan(fen: BoardFen, color: Color = chess.White, lastMove: Option[Uci] = None) =
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
