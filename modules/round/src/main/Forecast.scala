package lila.round

import org.joda.time.DateTime
import play.api.libs.json._

import chess.format.UciMove
import chess.Pos
import lila.game.{ Game, Move }

case class Forecast(
    _id: String, // player full id
    ply: Int,
    steps: Forecast.Steps,
    date: DateTime) {

  def apply(g: Game, lastMove: Move): (Forecast, Option[UciMove]) = (this, nextMove(g))

  private def nextMove(g: Game, last: Move) = steps.foldLeft(none[UciMove]) {
    case (None, fst :: snd :: rest) if => 
    case (found, _)                 => found
  }

}

object Forecast {

  type Steps = List[List[Step]]

  case class Step(
    ply: Int,
    uci: String,
    san: String,
    fen: String,
    check: Option[Boolean],
    dests: String)

  implicit val forecastStepJsonFormat = Json.format[Step]

  implicit val forecastJsonWriter = Json.writes[Forecast]

  case object OutOfSync extends lila.common.LilaException {
    val message = "Forecast out of sync"
  }
}
