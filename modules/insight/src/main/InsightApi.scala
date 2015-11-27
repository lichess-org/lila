package lila.insight

import reactivemongo.api.collections.bson.BSONBatchCommands.AggregationFramework._
import reactivemongo.bson._

import lila.db.Implicits._
import lila.game.GameRepo
import lila.user.User

final class InsightApi(
    storage: Storage,
    pipeline: AggregationPipeline) {

  import lila.insight.{ Dimension => D, Metric => M }
  import InsightApi._

  def ask[X](question: Question[X], user: User): Fu[Answer[X]] =
    storage.aggregate(pipeline(question, user.id)).map { res =>
      Answer(question, AggregationClusters(question, res))
    }

  def userStatus(user: User): Fu[UserStatus] =
    GameRepo lastFinishedRated user flatMap {
      case None => fuccess(UserStatus.NoGame)
      case Some(game) => storage fetchLast user map {
        case None => UserStatus.Empty
        case Some(entry) if entry.date isBefore game.createdAt => UserStatus.Stale
        case _ => UserStatus.Fresh
      }
    }
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
