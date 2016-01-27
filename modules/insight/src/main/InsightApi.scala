package lila.insight

import org.joda.time.DateTime
import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

import lila.db.Implicits._
import lila.game.{ Game, GameRepo, Pov }
import lila.user.User

final class InsightApi(
    storage: Storage,
    userCacheApi: UserCacheApi,
    pipeline: AggregationPipeline,
    indexer: Indexer) {

  import lila.insight.{ Dimension => D, Metric => M }
  import InsightApi._

  def userCache(user: User): Fu[UserCache] = userCacheApi find user.id flatMap {
    case Some(c) => fuccess(c)
    case None => for {
      count <- storage count user.id
      ecos <- storage ecos user.id
      c = UserCache(user.id, count, ecos, DateTime.now)
      _ <- userCacheApi save c
    } yield c
  }

  def ask[X](question: Question[X], user: User): Fu[Answer[X]] =
    storage.aggregate(pipeline(question, user.id)).flatMap { res =>
      val clusters = AggregationClusters(question, res)
      val gameIds = scala.util.Random.shuffle(clusters.flatMap(_.gameIds)) take 4
      GameRepo.userPovsByGameIds(gameIds, user) map { povs =>
        Answer(question, clusters, povs)
      }
    }

  def userStatus(user: User): Fu[UserStatus] =
    GameRepo lastFinishedRatedNotFromPosition user flatMap {
      case None => fuccess(UserStatus.NoGame)
      case Some(game) => storage fetchLast user map {
        case None => UserStatus.Empty
        case Some(entry) if entry.date isBefore game.createdAt => UserStatus.Stale
        case _ => UserStatus.Fresh
      }
    }

  def indexAll(user: User) =
    indexer.all(user) >> userCacheApi.remove(user.id)

  def updateGame(g: Game) = Pov(g).map { pov =>
    pov.player.userId ?? { userId =>
      storage find Entry.povToId(pov) flatMap {
        _ ?? { old =>
          indexer.update(g, userId, old)
        }
      }
    }
  }.sequenceFu.void
}

object InsightApi {

  sealed trait UserStatus
  object UserStatus {
    case object NoGame extends UserStatus
    case object Empty extends UserStatus
    case object Stale extends UserStatus
    case object Fresh extends UserStatus
  }
}
