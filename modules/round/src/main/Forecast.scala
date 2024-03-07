package lila.round

import org.joda.time.DateTime
import play.api.libs.json._

import shogi.format.usi.{ UciToUsi, Usi }
import lila.common.Json.jodaWrites
import lila.game.Game

case class Forecast(
    _id: String, // player full id
    steps: Forecast.Steps,
    date: DateTime
) {

  def apply(g: Game, lastUsi: Usi): Option[(Forecast, Usi)] =
    nextUsi(g, lastUsi) map { usi =>
      copy(
        steps = steps.collect {
          case (fst :: snd :: rest)
              if rest.nonEmpty && g.plies == fst.ply && fst.is(lastUsi) && snd.is(usi) =>
            rest
        },
        date = DateTime.now
      ) -> usi
    }
  // accept up to 30 lines of 30 moves each
  def truncate = copy(steps = steps.take(30).map(_ take 30))

  private def nextUsi(g: Game, last: Usi) =
    steps.foldLeft(none[Usi]) {
      case (None, fst :: snd :: _) if g.plies == fst.ply && fst.is(last) => snd.usiMove
      case (move, _)                                                     => move
    }
}

object Forecast {

  type Steps = List[List[Step]]

  def maxPlies(steps: Steps): Int = ~steps.map(_.size).sortBy(-_).lastOption

  case class Step(
      ply: Int,
      usi: String,
      sfen: String,
      check: Option[Boolean]
  ) {

    def is(move: Usi) = move.usi == usi

    def usiMove = Usi(usi).orElse(UciToUsi(usi))
  }

  implicit val forecastStepJsonFormat = Json.format[Step]

  implicit val forecastJsonWriter = Json.writes[Forecast]

  case object OutOfSync extends lila.base.LilaException {
    val message = "Forecast out of sync"
  }
}
