package lila.tv

import scala.concurrent.duration._
import scala.concurrent.Promise

import chess.Color
import lila.game.{ Game, GameRepo }
import lila.hub.Trouper

private[tv] final class ChannelTrouper(
    channel: Tv.Channel,
    lightUser: lila.common.LightUser.GetterSync,
    onSelect: TvTrouper.Selected => Unit
) extends Trouper {

  import ChannelTrouper._

  // games featured on this channel
  // first entry is the current game
  private var history = List.empty[Game.ID]

  private def oneId = history.headOption

  // the list of candidates by descending rating order
  private var manyIds = List.empty[Game.ID]

  protected val process: Trouper.Receive = {

    case GetGameId(promise) => promise success oneId

    case GetGameIdAndHistory(promise) => promise success GameIdAndHistory(oneId, history drop 1)

    case GetGameIds(max, promise) => promise success manyIds.take(max)

    case SetGame(game) =>
      onSelect(TvTrouper.Selected(channel, game))
      history = game.id :: history.take(2)

    case Select(candidates) => if (candidates.nonEmpty) {
      oneId ?? GameRepo.game map2 Tv.toCandidate(lightUser) foreach {
        case Some(current) if channel filter current =>
          wayBetter(current.game, candidates) orElse rematch(current.game) foreach elect
        case Some(current) => rematch(current.game) orElse feature(candidates) foreach elect
        case _ => feature(candidates) foreach elect
      }
      manyIds = candidates.sortBy { g =>
        -(~g.averageUsersRating)
      }.map(_.id)
    }
  }

  private def elect(gameOption: Option[Game]): Unit = gameOption foreach { this ! SetGame(_) }

  private def wayBetter(game: Game, candidates: List[Game]) = feature(candidates) map {
    case Some(next) if isWayBetter(game, next) => next.some
    case _ => none
  }

  private def isWayBetter(g1: Game, g2: Game) = score(g2.resetTurns) > (score(g1.resetTurns) * 1.15)

  private def rematch(game: Game) = game.next ?? GameRepo.game

  private def feature(candidates: List[Game]) = fuccess {
    candidates sortBy { -score(_) } headOption
  }

  private def score(game: Game): Int = math.round {
    (heuristics map {
      case (fn, coefficient) => heuristicBox(fn(game)) * coefficient
    }).sum * 1000
  }

  private type Heuristic = Game => Float
  private val heuristicBox = box(0 to 1) _
  private val ratingBox = box(1000 to 2700) _
  private val turnBox = box(1 to 25) _

  private val heuristics: List[(Heuristic, Float)] = List(
    ratingHeuristic(Color.White) -> 1.2f,
    ratingHeuristic(Color.Black) -> 1.2f,
    progressHeuristic -> 0.7f
  )

  private def ratingHeuristic(color: Color): Heuristic = game =>
    ratingBox(game.player(color).rating | 1400)

  private def progressHeuristic: Heuristic = game =>
    1 - turnBox(game.turns)

  // boxes and reduces to 0..1 range
  private def box(in: Range.Inclusive)(v: Float): Float =
    (math.max(in.start, math.min(v, in.end)) - in.start) / (in.end - in.start).toFloat
}

object ChannelTrouper {

  case class GetGameId(promise: Promise[Option[Game.ID]])
  case class GetGameIds(max: Int, promise: Promise[List[Game.ID]])
  private case class SetGame(game: Game)

  case class Select(candidates: List[Game])

  case class GetGameIdAndHistory(promise: Promise[GameIdAndHistory])
  case class GameIdAndHistory(gameId: Option[Game.ID], history: List[Game.ID])
}
