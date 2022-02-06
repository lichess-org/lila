package controllers

import shogi.format.forsyth.Sfen
import shogi.Situation
import play.api.libs.json._

import lila.app._
import views._
import lila.common.Json._

final class Editor(env: Env) extends LilaController(env) {

  private lazy val positionsJson = lila.common.String.html.safeJsonValue {
    JsArray(shogi.StartingPosition.all map { p =>
      Json.obj(
        "japanese" -> p.japanese,
        "english"  -> p.english,
        "sfen"     -> p.sfen
      )
    })
  }

  def index = load("")

  def load(urlSfen: String) =
    Open { implicit ctx =>
      val sfenStr = lila.common.String
        .decodeUriPath(urlSfen)
        .map(_.replace('_', ' ').trim)
        .filter(_.nonEmpty)
        .orElse(get("sfen"))
      fuccess {
        val situation = readSfen(sfenStr)
        Ok(
          html.board.editor(
            sit = situation,
            sfen = situation.toSfen,
            positionsJson,
            animationDuration = env.api.config.editorAnimationDuration
          )
        )
      }
    }

  def data =
    Open { implicit ctx =>
      fuccess {
        val situation = readSfen(get("sfen"))
        Ok(
          html.board.bits.jsData(
            sit = situation,
            sfen = situation.toSfen,
            animationDuration = env.api.config.editorAnimationDuration
          )
        ) as JSON
      }
    }

  private def readSfen(sfen: Option[String]): Situation =
    sfen.map(Sfen.clean).filter(_.value.nonEmpty).flatMap(_.toSituation(shogi.variant.Standard)) | Situation(
      shogi.variant.Standard
    )

  def game(id: String) =
    Open { implicit ctx =>
      OptionResult(env.game.gameRepo game id) { game =>
        Redirect {
          if (game.playable) routes.Round.watcher(game.id, "sente")
          else routes.Editor.load(get("sfen") | (game.shogi.toSfen).value)
        }
      }
    }
}
