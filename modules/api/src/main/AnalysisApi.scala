package lila.api

import play.api.libs.json._

import chess.format.pgn.Pgn
import lila.analyse.Analysis
import lila.common.PimpedJson._

private[api] final class AnalysisApi {

  def game(analysis: Analysis, pgn: Pgn) = JsArray(analysis.infoAdvices zip pgn.moves map {
    case ((info, adviceOption), move) => Json.obj(
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
}
