package controllers

import chess.format.{ FEN, Forsyth }
import chess.Situation
import chess.variant.Variant
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
          html.board.bits.editorJsData()
        )
      }
    }

  def game(id: String) =
    Open { implicit ctx =>
      OptionResult(env.game.gameRepo game id) { game =>
        Redirect {
          if (game.playable) routes.Round.watcher(game.id, "white").url
          else editorUrl(get("fen").fold(Forsyth >> game.chess)(FEN.apply), game.variant)
        }
      }
    }

  private[controllers] def editorUrl(fen: FEN, variant: Variant): String =
    if (fen == Forsyth.initial && variant.standard) routes.Editor.index.url
    else {
      val params = variant.exotic ?? s"?variant=${variant.key}"
      routes.Editor.load(fen.value.replace(" ", "_")).url + params
    }
}
