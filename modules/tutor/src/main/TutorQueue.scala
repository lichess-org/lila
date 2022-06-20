package lila.tutor

import com.softwaremill.tagging._
import org.joda.time.DateTime
import reactivemongo.api._
import reactivemongo.api.bson._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.common.IpAddress
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

final private class TutorQueue(
    builder: TutorBuilder,
    reportColl: Coll @@ ReportColl,
    queueColl: Coll @@ QueueColl,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext, system: akka.actor.ActorSystem) {

  import TutorQueue._

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 64, timeout = 5 seconds, "tutorQueue")

  private val durationCache = cacheApi.unit[FiniteDuration] {
    _.refreshAfterWrite(1 minutes)
      .buildAsyncFuture { _ =>
        reportColl
          .aggregateOne(ReadPreference.secondaryPreferred) { framework =>
            import framework._
            Sort(Descending(TutorReport.F.at)) -> List(
              Limit(100),
              Group(BSONNull)(TutorReport.F.millis -> AvgField(TutorReport.F.millis))
            )
          }
          .map {
            ~_.flatMap(_.getAsOpt[Int](TutorReport.F.millis))
          }
          .map(_.millis)
      }
  }

  def status(user: User): Fu[Status] = workQueue { fetchStatus(user) }

  def enqueue(user: User): Fu[Status] = workQueue {
    queueColl.insert
      .one($doc(F.id -> user.id, F.at -> DateTime.now))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(user)
  }

  private def fetchStatus(user: User): Fu[Status] =
    queueColl.primitiveOne[DateTime]($id(user.id), F.at) flatMap {
      case None => fuccess(NotInQueue)
      case Some(at) =>
        for {
          position    <- queueColl.countSel($doc(F.at $lte at))
          avgDuration <- durationCache.get({})
        } yield InQueue(position, avgDuration)
    }
}

object TutorQueue {

  sealed trait Status
  case object NotInQueue extends Status
  case class InQueue(position: Int, avgDuration: FiniteDuration) extends Status {
    def eta = avgDuration * position
  }

  object F {
    val id = "_id"
    val at = "_id"
  }
}
