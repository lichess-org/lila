package lila.round

import org.joda.time.DateTime
import play.api.libs.json._

import chess.Pos
import lila.game.Game

case class Forecast(
  _id: String, // player full id
  ply: Int,
  steps: List[Forecast.Step],
  date: DateTime)

object Forecast {

  case class Step(
    ply: Int,
    uci: String,
    san: String,
    fen: String,
    check: Option[Boolean],
    dests: String)

  implicit val forecastStepJsonReader = Json.reads[Step]
}
