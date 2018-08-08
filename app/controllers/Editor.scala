package controllers

import draughts.format.Forsyth
import draughts.Situation
import play.api.libs.json._
import play.twirl.api.Html

import lidraughts.app._
import lidraughts.game.GameRepo
import views._

object Editor extends LidraughtsController {

  private lazy val positionsJson = lidraughts.common.String.html.safeJson {
    JsArray(draughts.StartingPosition.all map { p =>
      Json.obj(
        "eco" -> p.eco,
        "name" -> p.name,
        "fen" -> p.fen
      )
    })
  }

  def index = load("")

  def load(urlFen: String) = Open { implicit ctx =>
    val fenStr = lidraughts.common.String.decodeUriPath(urlFen)
      .map(_.replace("_", " ").trim).filter(_.nonEmpty)
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
      Ok(html.board.JsData(
        sit = situation,
        fen = Forsyth >> situation,
        animationDuration = Env.api.EditorAnimationDuration
      )) as JSON
    }
  }

  private def readFen(fen: Option[String]): Situation =
    fen.map(_.trim).filter(_.nonEmpty).flatMap(Forsyth.<<<).map(_.situation) | Situation(draughts.variant.Standard)

  def game(id: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect {
        if (game.playable) routes.Round.watcher(game.id, "white")
        else routes.Editor.load(get("fen") | (draughts.format.Forsyth >> game.draughts))
      }
    }
  }
}
