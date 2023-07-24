package lila.tv

import scala.concurrent.duration._
import scala.concurrent.Promise

import shogi.Color
import lila.game.Game
import lila.hub.Trouper

final private[tv] class ChannelTrouper(
    channel: Tv.Channel,
    onSelect: TvTrouper.Selected => Unit,
    proxyGame: Game.ID => Fu[Option[Game]],
    rematchOf: Game.ID => Option[Game.ID]
)(implicit ec: scala.concurrent.ExecutionContext)
    extends Trouper {

  import ChannelTrouper._

  // games featured on this channel
  // first entry is the current game
  private var history = List.empty[Game.ID]

  private def oneId = history.headOption

  // the list of candidates by descending rating order
  private var manyIds = List.empty[Game.ID]

  private val candidateIds = new lila.memo.ExpireSetMemo(20 minutes)

  protected val process: Trouper.Receive = {

    case GetGameId(promise) => promise success oneId

    case GetGameIdAndHistory(promise) => promise success GameIdAndHistory(oneId, history drop 1)

    case GetGameIds(max, promise) => promise success manyIds.take(max)

    case SetGame(game) =>
      onSelect(TvTrouper.Selected(channel, game))
      history = game.id :: history.take(2)

    case TvTrouper.Select =>
      candidateIds.keys
        .map(proxyGame)
        .sequenceFu
        .map(
          _.view
            .collect {
              case Some(g) if isActive(g) => g
            }
            .toList
        )
        .foreach { candidates =>
          oneId ?? proxyGame foreach {
            case Some(game) if isActive(game) =>
              fuccess(wayBetter(game, candidates)) orElse rematch(game) foreach elect
            case Some(game) => rematch(game) orElse fuccess(bestOf(candidates)) foreach elect
            case _          => elect(bestOf(candidates))
          }
          manyIds = candidates
            .sortBy { g =>
              -(~g.averageUsersRating(0))
            }
            .take(50)
            .map(_.id)
        }
  }

  def addCandidate(game: Game): Unit = candidateIds put game.id

  private def elect(gameOption: Option[Game]): Unit = gameOption foreach { this ! SetGame(_) }

  private def wayBetter(game: Game, candidates: List[Game]) =
    bestOf(candidates) filter { isWayBetter(game, _) }

  private def isWayBetter(g1: Game, g2: Game) = score(g2) > (score(g1) * 1.17)

  private def rematch(game: Game): Fu[Option[Game]] = rematchOf(game.id) ?? proxyGame

  private def bestOf(candidates: List[Game]) = {
    import cats.implicits._
    candidates.maximumByOption(score)
  }

  private def score(game: Game): Int =
    heuristics.foldLeft(0) { case (score, fn) =>
      score + fn(game)
    }

  private def isActive(game: Game): Boolean =
    game.onePlayerHasMoved && (game.isBeingPlayed || (game.finished && !game.olderThan(20)))

  private type Heuristic = Game => Int

  private val heuristics: List[Heuristic] = List(
    ratingHeuristic(Color.Sente),
    ratingHeuristic(Color.Gote),
    speedHeuristic,
    sourceHeuristic
  )

  private def ratingHeuristic(color: Color): Heuristic =
    game => game.player(color).stableRating | 1250

  // prefer faster games - better for watching
  private def speedHeuristic: Heuristic =
    game => ~game.estimateClockTotalTime.map(ct => ((1000 - ct) / 5) atLeast 0) + (!game.olderThan(60) ?? 750)

  private def sourceHeuristic: Heuristic =
    game => {
      if (game.source.contains(lila.game.Source.Api)) 0
      else if (game.source.contains(lila.game.Source.Ai)) 125
      else if (game.source.contains(lila.game.Source.Friend)) 250 // bots
      else 1000
    }

}

object ChannelTrouper {

  case class GetGameId(promise: Promise[Option[Game.ID]])
  case class GetGameIds(max: Int, promise: Promise[List[Game.ID]])
  private case class SetGame(game: Game)

  case class GetGameIdAndHistory(promise: Promise[GameIdAndHistory])
  case class GameIdAndHistory(gameId: Option[Game.ID], history: List[Game.ID])
}
