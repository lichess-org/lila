package lila.tv

import akka.actor._
import scala.concurrent.duration._

import chess.Color
import lila.game.{ Game, GameRepo }

private[tv] final class ChannelActor(channel: Tv.Channel) extends Actor {

  import ChannelActor._

  implicit def timeout = makeTimeout(50 millis)

  // games featured on this channel
  // first entry is the current game
  var history = List.empty[Game.ID]

  def oneId = history.headOption

  // the list of candidates by descending rating order
  var manyIds = List.empty[Game.ID]

  def receive = {

    case GetGameId => sender ! oneId

    case GetGameIdAndHistory => sender ! GameIdAndHistory(oneId, history drop 1)

    case GetGameIds(max) => sender ! manyIds.take(max)

    case SetGame(game) =>
      context.parent ! TvActor.Selected(channel, game, oneId)
      history = game.id :: history.take(2)

    case Select(candidates) => if (candidates.nonEmpty) {
      oneId ?? GameRepo.game foreach {
        case Some(game) if channel.filter(game) =>
          wayBetter(game, candidates) orElse rematch(game) foreach elect
        case Some(game) => rematch(game) orElse feature(candidates) foreach elect
        case _ => feature(candidates) foreach elect
      }
      manyIds = candidates.sortBy { g =>
        -(~g.averageUsersRating)
      }.map(_.id)
    }
  }

  def elect(gameOption: Option[Game]): Unit = gameOption foreach { self ! SetGame(_) }

  def wayBetter(game: Game, candidates: List[Game]) = feature(candidates) map {
    case Some(next) if isWayBetter(game, next) => next.some
    case _ => none
  }

  def isWayBetter(g1: Game, g2: Game) = score(g2.resetTurns) > (score(g1.resetTurns) * 1.15)

  def rematch(game: Game) = game.next ?? GameRepo.game

  def feature(candidates: List[Game]) = fuccess {
    candidates sortBy { -score(_) } headOption
  }

  def score(game: Game): Int = math.round {
    (heuristics map {
      case (fn, coefficient) => heuristicBox(fn(game)) * coefficient
    }).sum * 1000
  }

  type Heuristic = Game => Float
  val heuristicBox = box(0 to 1) _
  val ratingBox = box(1000 to 2700) _
  val turnBox = box(1 to 25) _

  val heuristics: List[(Heuristic, Float)] = List(
    ratingHeuristic(Color.White) -> 1.2f,
    ratingHeuristic(Color.Black) -> 1.2f,
    progressHeuristic -> 0.7f
  )

  def ratingHeuristic(color: Color): Heuristic = game =>
    ratingBox(game.player(color).rating | 1400)

  def progressHeuristic: Heuristic = game =>
    1 - turnBox(game.turns)

  // boxes and reduces to 0..1 range
  def box(in: Range.Inclusive)(v: Float): Float =
    (math.max(in.start, math.min(v, in.end)) - in.start) / (in.end - in.start).toFloat
}

object ChannelActor {

  case object GetGameId
  case class GetGameIds(max: Int) extends AnyVal
  private case class SetGame(game: Game) extends AnyVal

  case class Select(candidates: List[Game]) extends AnyVal

  case object GetGameIdAndHistory
  case class GameIdAndHistory(gameId: Option[Game.ID], history: List[Game.ID])
}
