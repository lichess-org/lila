package lila.round

import org.joda.time.DateTime
import play.api.libs.json._

import chess.format.Uci
import lila.common.Json.jodaWrites
import lila.game.Game

case class Forecast(
    _id: String, // player full id
    steps: Forecast.Steps,
    date: DateTime
) {

  def apply(g: Game, lastMove: Uci): Option[(Forecast, Uci)] =
    nextMove(g, lastMove) map { move =>
      copy(
        steps = steps.collect {
          case (fst :: snd :: rest)
              if rest.nonEmpty && g.turns == fst.ply && fst.is(lastMove) && snd.is(move) =>
            rest
        },
        date = DateTime.now
      ) -> move
    }
  // accept up to 30 lines of 30 moves each
  def truncate = copy(steps = steps.take(30).map(_ take 30))

  private def nextMove(g: Game, last: Uci) =
    steps.foldLeft(none[Uci]) {
      case (None, fst :: snd :: _) if g.turns == fst.ply && fst.is(last) => snd.uciMove
      case (move, _)                                                     => move
    }
  }

object Forecast {

  type Steps = List[List[Step]]

  def maxPlies(steps: Steps): Int = ~steps.map(_.size).sortBy(-_).lastOption

  case class Step(
      ply: Int,
      uci: String,
      san: String,
      fen: String,
      check: Option[Boolean]
  ) {

    def is(move: Uci) = move.uci == uci

    def uciMove = Uci(uci)
  }

  implicit val forecastStepJsonFormat = Json.format[Step]

  implicit val forecastJsonWriter = Json.writes[Forecast]

  case object OutOfSync extends lila.base.LilaException {
    val message = "Forecast out of sync"
  }
}
