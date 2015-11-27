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
      val clusters = res.documents.flatMap { doc =>
        for {
          id <- doc.getAs[X]("_id")(question.dimension.bson)
          value <- doc.getAs[BSONNumberLike]("v")
          nb <- doc.getAs[Int]("nb")
        } yield Cluster(id,
          Point.Data(question.metric.name, value.toDouble),
          Point.Size(question.metric.position.tellNumber, nb))
      }
      Answer(question, postSort(question)(clusters))
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

  private def postSort[X](q: Question[X])(clusters: List[Cluster[X]]): List[Cluster[X]] = q.dimension match {
    case D.Opening => clusters
    case _         => sortLike[Cluster[X], X](clusters, D.valuesOf(q.dimension), _.x)
  }

  private def sortLike[A, B](la: List[A], lb: List[B], f: A => B): List[A] = la.sortWith {
    case (x, y) => lb.indexOf(f(x)) < lb.indexOf(f(y))
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
