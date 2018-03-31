package lila.insight

import org.joda.time.DateTime

import lila.game.{ Game, GameRepo, Pov }
import lila.user.User

final class InsightApi(
    storage: Storage,
    userCacheApi: UserCacheApi,
    pipeline: AggregationPipeline,
    indexer: Indexer
) {

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
    storage.aggregate(pipeline(question, user.id)).flatMap { aggDocs =>
      val clusters = AggregationClusters(question, aggDocs)
      val gameIds = scala.util.Random.shuffle(clusters.flatMap(_.gameIds)) take 4
      GameRepo.userPovsByGameIds(gameIds, user) map { povs =>
        Answer(question, clusters, povs)
      }
    }.mon(_.insight.request.time) >>- lila.mon.insight.request.count()

  def userStatus(user: User): Fu[UserStatus] =
    GameRepo lastFinishedRatedNotFromPosition user flatMap {
      case None => fuccess(UserStatus.NoGame)
      case Some(game) => storage fetchLast user.id map {
        case None => UserStatus.Empty
        case Some(entry) if entry.date isBefore game.createdAt => UserStatus.Stale
        case _ => UserStatus.Fresh
      }
    }

  def indexAll(user: User) =
    indexer.all(user).mon(_.insight.index.time) >>
      userCacheApi.remove(user.id) >>-
      lila.mon.insight.index.count()

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
