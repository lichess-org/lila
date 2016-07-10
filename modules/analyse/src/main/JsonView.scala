package lila.analyse

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.Game

object JsonView {

  def game(analysis: Analysis) = JsArray(analysis.infoAdvices map {
    case ((info, adviceOption)) => Json.obj(
      "eval" -> info.score.map(_.centipawns),
      "mate" -> info.mate,
      "variation" -> info.variation.isEmpty.fold(JsNull, info.variation mkString " "),
      "comment" -> adviceOption.map(_.makeComment(false, true))
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
}
