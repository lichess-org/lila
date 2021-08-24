package controllers

import chess.format.{ FEN, Forsyth }
import chess.Situation
import play.api.libs.json._
import views._

import lila.app._
import lila.common.Json._

final class Editor(env: Env) extends LilaController(env) {

  private lazy val positionsJson = lila.common.String.html.safeJsonValue {
    JsArray(chess.StartingPosition.all map { p =>
      Json.obj(
        "eco"  -> p.eco,
        "name" -> p.name,
        "fen"  -> p.fen
      )
    })
  }

  private lazy val endgamePositionsJson = lila.common.String.html.safeJsonValue {
    JsArray(chess.EndgamePosition.positions map { p =>
      Json.obj(
        "name" -> p.name,
        "fen"  -> p.fen
      )
    })
  }

  def index = load("")

  def load(urlFen: String) =
    Open { implicit ctx =>
      val fen = lila.common.String
        .decodeUriPath(urlFen)
        .map(_.replace('_', ' ').trim)
        .filter(_.nonEmpty)
      fuccess {
        Ok(
          html.board.editor(
            fen,
            positionsJson,
            endgamePositionsJson
          )
        )
      }
    }

  def data =
    Open { implicit ctx =>
      fuccess {
        JsonOk(
          html.board.bits.jsData()
        )
      }
    }

  def game(id: String) =
    Open { implicit ctx =>
      OptionResult(env.game.gameRepo game id) { game =>
        Redirect {
          if (game.playable) routes.Round.watcher(game.id, "white")
          else routes.Editor.load(get("fen") | (chess.format.Forsyth >> game.chess).value)
        }
      }
    }
}
