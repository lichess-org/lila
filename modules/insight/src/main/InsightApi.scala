package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.bson.BSONNull

import lila.game.{ Game, GameRepo, Pov }
import lila.user.User
import lila.common.config
import lila.common.Heapsort.implicits._

final class InsightApi(
    storage: InsightStorage,
    pipeline: AggregationPipeline,
    insightUserApi: InsightUserApi,
    gameRepo: GameRepo,
    indexer: InsightIndexer
)(implicit ec: scala.concurrent.ExecutionContext) {

  import InsightApi._

  def insightUser(user: User): Fu[InsightUser] =
    insightUserApi find user.id flatMap {
      case Some(u) =>
        u.lastSeen.isBefore(DateTime.now minusDays 1) ?? {
          insightUserApi setSeenNow user
        } inject u
      case None =>
        for {
          count                <- storage count user.id
          (families, openings) <- storage openings user.id
          c = InsightUser.make(user.id, count, families, openings)
          _ <- insightUserApi save c
        } yield c
    }

  def ask[X](question: Question[X], user: User, withPovs: Boolean = true): Fu[Answer[X]] =
    pipeline
      .aggregate(question, Left(user), withPovs = withPovs)
      .flatMap { aggDocs =>
        val clusters = AggregationClusters(question, aggDocs)
        withPovs ?? {
          gameRepo.userPovsByGameIds(clusters.flatMap(_.gameIds) botN 4, user)
        } map { Answer(question, clusters, _) }
      }
      .monSuccess(_.insight.user)

  def askPeers[X](question: Question[X], rating: MeanRating, nbGames: config.Max): Fu[Answer[X]] =
    pipeline
      .aggregate(question, Right(Question.Peers(rating)), withPovs = false, nbGames = nbGames)
      .map { aggDocs =>
        Answer(question, AggregationClusters(question, aggDocs), Nil)
      }
      .monSuccess(_.insight.peers)

  def userStatus(user: User): Fu[UserStatus] =
    gameRepo lastFinishedRatedNotFromPosition user flatMap {
      case None => fuccess(UserStatus.NoGame)
      case Some(game) =>
        storage fetchLast user.id map {
          case None                                              => UserStatus.Empty
          case Some(entry) if entry.date isBefore game.createdAt => UserStatus.Stale
          case _                                                 => UserStatus.Fresh
        }
    }

  def indexAll(user: User) =
    indexer
      .all(user)
      .monSuccess(_.insight.index) >>
      insightUserApi.remove(user.id)

  def updateGame(g: Game) =
    Pov(g)
      .map { pov =>
        pov.player.userId ?? { userId =>
          storage find InsightEntry.povToId(pov) flatMap {
            _ ?? { indexer.update(g, userId, _) }
          }
        }
      }
      .sequenceFu
      .void

  def coll = storage.coll
}

object InsightApi {

  sealed trait UserStatus
  object UserStatus {
    case object NoGame extends UserStatus // the user has no rated games
    case object Empty  extends UserStatus // insights not yet generated
    case object Stale  extends UserStatus // new games not yet generated
    case object Fresh  extends UserStatus // up to date
  }
}
