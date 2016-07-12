package lila.analyse

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.Game

object JsonView {

  def moves(analysis: Analysis) = JsArray(analysis.infoAdvices map {
    case ((info, adviceOption)) => Json.obj(
      "eval" -> info.score.map(_.centipawns),
      "mate" -> info.mate,
      "best" -> info.best.map(_.uci),
      "variation" -> info.variation.nonEmpty.option(info.variation mkString " "),
      "judgment" -> adviceOption.map { a =>
        Json.obj(
          "glyph" -> Json.obj(
            "name" -> a.judgment.glyph.name,
            "symbol" -> a.judgment.glyph.symbol
          ),
          "name" -> a.judgment.name,
          "comment" -> a.makeComment(false, true)
        )
      }
    ).noNull
  })

  def player(pov: lila.game.Pov)(analysis: Analysis) =
    analysis.summary.find(_._1 == pov.color).map(_._2).map(s =>
      JsObject(s map {
        case (nag, nb) => nag.toString.toLowerCase -> JsNumber(nb)
      }) ++ lila.analyse.Accuracy.mean(pov, analysis).fold(Json.obj()) { acpl =>
        Json.obj("acpl" -> acpl)
      }
    )

  def bothPlayers(game: Game, analysis: Analysis) = Json.obj(
    "white" -> player(game.whitePov)(analysis),
    "black" -> player(game.blackPov)(analysis))

  def mobile(game: Game, analysis: Analysis) = Json.obj(
    "summary" -> bothPlayers(game, analysis),
    "moves" -> moves(analysis)
  )
}
