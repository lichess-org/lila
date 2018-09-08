package controllers

import draughts.format.Forsyth
import draughts.Situation
import draughts.variant.{ Standard, Variant }
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

  def parse(arg: String) = arg.split("/", 2) match {
    case Array(key) => Variant(key) match {
      case Some(variant) => load("", variant)
      case _ => load(arg, Standard)
    }
    case Array(key, fen) => Variant.byKey get key match {
      case Some(variant) => load(fen, variant)
      case _ => load(arg, Standard)
    }
    case _ => load(arg, Standard)
  }

  def index = load("", Standard)

  def load(urlFen: String, variant: Variant) = Open { implicit ctx =>
    val fenStr = lidraughts.common.String.decodeUriPath(urlFen)
      .map(_.replace("_", " ").trim).filter(_.nonEmpty)
      .orElse(get("fen"))
    fuccess {
      val situation = readFen(fenStr, (get("variant") flatMap { Variant.byKey.get }).getOrElse(variant).some)
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
      val situation = readFen(get("fen"), get("variant") flatMap { Variant.byKey.get })
      Ok(html.board.JsData(
        sit = situation,
        fen = Forsyth >> situation,
        animationDuration = Env.api.EditorAnimationDuration
      )) as JSON
    }
  }

  private def readFen(fen: Option[String], variant: Option[Variant]): Situation =
    fen.map(_.trim).filter(_.nonEmpty).flatMap(Forsyth.<<<@(variant.getOrElse(Standard), _)).map(_.situation) | Situation(variant.getOrElse(Standard))

  def game(id: String) = Open { implicit ctx =>
    OptionResult(GameRepo game id) { game =>
      Redirect {
        if (game.playable) routes.Round.watcher(game.id, "white")
        else routes.Editor.parse(s"${game.variant.key}/${get("fen") | (draughts.format.Forsyth >> game.draughts)}")
      }
    }
  }
}
