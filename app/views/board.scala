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
            "wiki" -> pov.game.variant.standard
          )
          .add("inlinePgn", inlinePgn) ++
          explorerAndCevalConfig
      )
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
