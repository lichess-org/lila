package lidraughts.round

import org.joda.time.DateTime
import play.api.libs.json._

import draughts.format.{ Uci, Forsyth }
import draughts.Move
import lidraughts.game.Game

case class Forecast(
    _id: String, // player full id
    steps: Forecast.Steps,
    date: DateTime
) {

  def apply(g: Game, lastMove: Move): Option[(Forecast, Uci.Move)] =
    nextMove(g, lastMove) map { move =>
      copy(
        steps = steps.collect {
          case (fst :: snd :: rest) if rest.nonEmpty && g.turns == fst.ply && g.situation.captureLengthFrom(snd.uciMove.get.orig).getOrElse(0) > 1 && fst.is(lastMove.toShortUci) && snd.is(move) => snd :: rest
          case (fst :: snd :: rest) if rest.nonEmpty && g.turns == fst.ply && fst.is(lastMove.toShortUci) && snd.is(move) => rest
        },
        date = DateTime.now
      ) -> move
    }

  def moveOpponent(g: Game, lastMove: Move): Option[(Forecast, Uci.Move)] =
    nextMove(g, lastMove) map { move =>
      copy(
        steps = steps.collect {
          case (fst :: snd :: rest) if rest.nonEmpty && g.turns == fst.ply && fst.is(lastMove.toShortUci) && snd.is(move) => snd :: rest
        },
        date = DateTime.now
      ) -> lastMove.toShortUci
    }

  // accept up to 30 lines of 30 moves each
  def truncate = copy(steps = steps.take(30).map(_ take 30))

  private def nextMove(g: Game, last: Move) = steps.foldLeft(none[Uci.Move]) {
    case (None, fst :: snd :: _) if g.turns == fst.ply && fst.is(last.toShortUci) => snd.uciMove
    case (move, _) => move
  }
}

object Forecast {

  type Steps = List[List[Step]]

  def maxPlies(steps: Steps): Int = ~steps.map(_.size).sortBy(-_).lastOption

  case class Step(
      ply: Int,
      uci: String,
      san: String,
      fen: String
  ) {

    def is(move: Uci.Move) = move.uci == uci

    def uciMove = Uci.Move(uci)

    def displayPly = if (Forsyth.countGhosts(fen) > 0) ply + 1 else ply

  }

  implicit val forecastStepJsonFormat = Json.format[Step]

  implicit val forecastJsonWriter = Json.writes[Forecast]

  case object OutOfSync extends lidraughts.base.LidraughtsException {
    val message = "Forecast out of sync"
  }
}
