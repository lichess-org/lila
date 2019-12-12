package controllers

import chess.format.Forsyth
import chess.Situation
import play.api.libs.json._

import lila.app._
import lila.game.GameRepo
import views._

object Editor extends LilaController {

  private lazy val positionsJson = lila.common.String.html.safeJsonValue {
    JsArray(chess.StartingPosition.all map { p =>
      Json.obj(
        "eco" -> p.eco,
        "name" -> p.name,
        "fen" -> p.fen
      )
    })
  }

  def index = load("")

  def load(urlFen: String) = Open { implicit ctx =>
    val fenStr = lila.common.String.decodeUriPath(urlFen)
      .map(_.replace('_', ' ').trim).filter(_.nonEmpty)
      .orElse(get("fen"))
    fuccess {
      val situation = readFen(fenStr)
      Ok(html.board.editor(
        sit = situation,
        fen = Forsyth >> situation,
        positionsJson,
        animationDuration = Env.api.EditorAnimationDuration
      ))
    }
  }

  def data = Open { implicit ctx =>
    fuccess {
      val situation = readFen(get("fen"))
      Ok(html.board.bits.jsData(
        sit = situation,
        fen = Forsyth >> situation,
        animationDuration = Env.api.EditorAnimationDuration
      )) as JSON
    }
  }

  private def readFen(fen: Option[String]): Situation =
    fen.map(_.trim).filter(_.nonEmpty).flatMap(Forsyth.<<<).map(_.situation) | Situation(chess.variant.Standard)

  def game(id: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect {
        if (game.playable) routes.Round.watcher(game.id, "white")
        else routes.Editor.load(get("fen") | (chess.format.Forsyth >> game.chess))
      }
    }
  }
}
