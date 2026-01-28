package lila.insight

import scalalib.HeapSort.botN

import lila.game.GameRepo
import lila.core.perm.Granter

final class InsightApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline,
    gameRepo: GameRepo,
    indexer: InsightIndexer,
    cacheApi: lila.memo.CacheApi
)(using Executor)
    extends lila.game.core.insight.InsightApi:

  import InsightApi.*

  private val userCache = cacheApi[UserId, InsightUser](1024, "insight.user"):
    _.expireAfterWrite(15.minutes).maximumSize(4096).buildAsyncFuture(computeUser)

  private def computeUser(userId: UserId): Fu[InsightUser] =
    storage
      .count(userId)
      .flatMap:
        case 0 => fuccess(InsightUser(0, Nil, Nil))
        case count =>
          storage.openings(userId).map { (families, openings) =>
            InsightUser(count, families, openings)
          }
  private given Ordering[GameId] = stringOrdering

  def insightUser(user: User): Fu[InsightUser] = userCache.get(user.id)

  def ask[X](question: Question[X], user: User, withPovs: Boolean = true): Fu[Answer[X]] =
    pipeline
      .aggregate(question, Left(user), withPovs = withPovs)
      .flatMap { aggDocs =>
        val clusters = AggregationClusters(question, aggDocs)
        withPovs
          .so:
            gameRepo.userPovsByGameIds(clusters.flatMap(_.gameIds).botN(4), user)
          .map { Answer(question, clusters, _) }
      }
      .monSuccess(_.insight.user)

  def askPeers[X](question: Question[X], rating: MeanRating, nbGames: Max): Fu[Answer[X]] =
    pipeline
      .aggregate(question, Right(PeersRatingRange.of(rating)), withPovs = false, nbGames = nbGames)
      .map: aggDocs =>
        Answer(question, AggregationClusters(question, aggDocs), Nil)
      .monSuccess(_.insight.peers)

  def userStatus(user: User): Fu[UserStatus] =
    gameRepo
      .lastFinishedRatedNotFromPosition(user)
      .flatMap:
        case None => fuccess(UserStatus.NoGame)
        case Some(game) =>
          storage
            .fetchLast(user.id)
            .map:
              case None => UserStatus.Empty
              case Some(entry) if entry.date.isBefore(game.createdAt) => UserStatus.Stale
              case _ => UserStatus.Fresh

  def indexAll(user: User)(using me: Me) =
    for _ <- indexer.all(user, force = Granter(_.Appeals)).monSuccess(_.insight.index)
    yield userCache.put(user.id, computeUser(user.id))

  def updateGame(g: Game) =
    lila.game.Pov.list(g).sequentiallyVoid { pov =>
      pov.player.userId.so: userId =>
        storage
          .find(InsightEntry.povToId(pov))
          .flatMapz:
            indexer.update(g, userId, _)
    }

  def coll = storage.coll

object InsightApi:

  sealed trait UserStatus
  object UserStatus:
    case object NoGame extends UserStatus // the user has no rated games
    case object Empty extends UserStatus // insights not yet generated
    case object Stale extends UserStatus // new games not yet generated
    case object Fresh extends UserStatus // up to date
