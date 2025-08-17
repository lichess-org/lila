package lila.tv

import chess.{ Color, Ply }
import chess.IntRating
import monocle.syntax.all.*
import scalalib.actor.SyncActor
import scalalib.HeapSort.topNToList

import lila.core.LightUser

final private class ChannelSyncActor(
    channel: Tv.Channel,
    onSelect: TvSyncActor.Selected => Unit,
    gameProxy: lila.core.game.GameProxy,
    rematchOf: GameId => Option[GameId],
    lightUserSync: LightUser.GetterSync,
    userApi: lila.core.user.UserApi
)(using Executor)
    extends SyncActor:

  import ChannelSyncActor.*

  // games featured on this channel
  // first entry is the current game
  private var history = List.empty[GameId]

  private def oneId = history.headOption

  // the list of candidates by descending rating order
  private var manyIds = List.empty[GameId]

  private val candidateIds = scalalib.cache.ExpireSetMemo[GameId](3.minutes)

  protected val process: SyncActor.Receive =

    case GetGameId(promise) => promise.success(oneId)

    case GetGameIdAndHistory(promise) => promise.success(GameIdAndHistory(oneId, history.drop(1)))

    case GetGameIds(max, promise) => promise.success(manyIds.take(max))

    case GetReplacementGameId(oldId, exclude, promise) =>
      promise.success { (rematchOf(oldId) ++ manyIds).find { !exclude.contains(_) } }

    case SetGame(game) =>
      onSelect(TvSyncActor.Selected(channel, game))
      history = game.id :: history.take(2)
      lila.mon.tv.selector.rating(channel.name).record(game.averageUsersRating.so(_.value))

    case TvSyncActor.Select =>
      lila.mon.tv.selector.candidates(channel.name).record(candidateIds.count)
      doSelectNow().foreach: (one, many) =>
        manyIds = many
        one.foreach: game =>
          this ! SetGame(game)

  def addCandidate(game: Game): Unit = candidateIds.put(game.id)

  private val ratingOrdering = Ordering.by[Game, Int](_.averageUsersRating.so(_.value))

  private def doSelectNow(): Fu[(Option[Game], List[GameId])] = for
    allCandidates <- candidateIds.keys.parallel(gameProxy.gameIfPresent)
    freshCandidates = allCandidates.view.collect:
      case Some(g) if channel.isFresh(g) => g
    sortedCandidates = topNToList(freshCandidates, 64)(using ratingOrdering)
    cheaters <- userApi.filterEngines(sortedCandidates.flatMap(_.userIds))
    candidates = sortedCandidates.filterNot(_.userIds.toSet.intersect(cheaters).nonEmpty)
    _ = lila.mon.tv.selector.cheats(channel.name).record(sortedCandidates.size - candidates.size)
    currentBest <- oneId.so(gameProxy.gameIfPresent)
    newBest <- currentBest match
      case Some(current) if channel.isFresh(current) =>
        fuccess(wayBetter(current, candidates)).orElse(rematch(current))
      case Some(current) => rematch(current).orElse(fuccess(bestOf(candidates)))
      case _ => fuccess(bestOf(candidates))
  yield newBest -> candidates.map(_.id)

  private def wayBetter(game: Game, candidates: List[Game]) =
    bestOf(candidates).filter { isWayBetter(game, _) }

  private def resetTurns(g: Game): Game =
    g.focus(_.chess).modify(c => c.copy(ply = Ply.initial, startedAtPly = Ply.initial))

  private def isWayBetter(g1: Game, g2: Game) = score(resetTurns(g2)) > (score(resetTurns(g1)) * 1.17)

  private def rematch(game: Game): Fu[Option[Game]] = rematchOf(game.id).so(gameProxy.game)

  private def bestOf(candidates: List[Game]) =
    candidates.maximumByOption(score)

  private def score(game: Game): Int =
    heuristics.foldLeft(0): (score, fn) =>
      score + fn(game)

  private type Heuristic = Game => Int

  private val heuristics: List[Heuristic] = List(
    ratingHeuristic(Color.White),
    ratingHeuristic(Color.Black),
    titleHeuristic(Color.White),
    titleHeuristic(Color.Black)
  )

  private def ratingHeuristic(color: Color): Heuristic =
    game => game.player(color).stableRating.fold(1300)(_.value)

  private def titleHeuristic(color: Color): Heuristic = game =>
    ~game
      .player(color)
      .some
      .flatMap: p =>
        p.stableRating.exists(_ > IntRating(2100)).so(p.userId)
      .flatMap(lightUserSync)
      .flatMap(_.title)
      .flatMap(Tv.titleScores.get)

object ChannelSyncActor:

  case class GetGameId(promise: Promise[Option[GameId]])
  case class GetGameIds(max: Int, promise: Promise[List[GameId]])
  case class GetReplacementGameId(oldId: GameId, exclude: List[GameId], promise: Promise[Option[GameId]])
  private case class SetGame(game: Game)

  case class GetGameIdAndHistory(promise: Promise[GameIdAndHistory])
  case class GameIdAndHistory(gameId: Option[GameId], history: List[GameId])
