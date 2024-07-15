package lila.tv

import chess.{ Color, Ply }
import monocle.syntax.all.*
import scalalib.actor.SyncActor

import lila.core.LightUser

final private[tv] class ChannelSyncActor(
    channel: Tv.Channel,
    onSelect: TvSyncActor.Selected => Unit,
    proxyGame: GameId => Fu[Option[Game]],
    rematchOf: GameId => Option[GameId],
    lightUserSync: LightUser.GetterSync
)(using Executor)
    extends SyncActor:

  import ChannelSyncActor.*

  // games featured on this channel
  // first entry is the current game
  private var history = List.empty[GameId]

  private def oneId = history.headOption

  // the list of candidates by descending rating order
  private var manyIds = List.empty[GameId]

  private val candidateIds = scalalib.cache.ExpireSetMemo[GameId](3 minutes)

  protected val process: SyncActor.Receive =

    case GetGameId(promise) => promise.success(oneId)

    case GetGameIdAndHistory(promise) => promise.success(GameIdAndHistory(oneId, history.drop(1)))

    case GetGameIds(max, promise) => promise.success(manyIds.take(max))

    case GetReplacementGameId(oldId, exclude, promise) =>
      promise.success { (rematchOf(oldId) ++ manyIds).find { !exclude.contains(_) } }

    case SetGame(game) =>
      onSelect(TvSyncActor.Selected(channel, game))
      history = game.id :: history.take(2)

    case TvSyncActor.Select =>
      candidateIds.keys
        .parallel(proxyGame)
        .map(
          _.view
            .collect {
              case Some(g) if channel.isFresh(g) => g
            }
            .toList
        )
        .foreach { candidates =>
          oneId.so(proxyGame).foreach {
            case Some(current) if channel.isFresh(current) =>
              fuccess(wayBetter(current, candidates)).orElse(rematch(current)).foreach(elect)
            case Some(current) => rematch(current).orElse(fuccess(bestOf(candidates))).foreach(elect)
            case _             => elect(bestOf(candidates))
          }
          manyIds = candidates
            .sortBy: g =>
              -(~g.averageUsersRating)
            .take(50)
            .map(_.id)
        }

  def addCandidate(game: Game): Unit = candidateIds.put(game.id)

  private def elect(gameOption: Option[Game]): Unit = gameOption.foreach { this ! SetGame(_) }

  private def wayBetter(game: Game, candidates: List[Game]) =
    bestOf(candidates).filter { isWayBetter(game, _) }

  private def resetTurns(g: Game): Game =
    g.focus(_.chess).modify(c => c.copy(ply = Ply.initial, startedAtPly = Ply.initial))

  private def isWayBetter(g1: Game, g2: Game) = score(resetTurns(g2)) > (score(resetTurns(g1)) * 1.17)

  private def rematch(game: Game): Fu[Option[Game]] = rematchOf(game.id).so(proxyGame)

  private def bestOf(candidates: List[Game]) =
    candidates.maximumByOption(score)

  private def score(game: Game): Int =
    heuristics.foldLeft(0) { case (score, fn) =>
      score + fn(game)
    }

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
      .flatMap { p =>
        p.stableRating.exists(_ > 2100).so(p.userId)
      }
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
