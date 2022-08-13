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
    reportColl: Coll @@ ReportColl,
    queueColl: Coll @@ QueueColl,
    cacheApi: CacheApi
)(implicit ec: ExecutionContext, scheduler: akka.actor.Scheduler) {

  import TutorQueue._

  private val workQueue =
    new lila.hub.AsyncActorSequencer(maxSize = 64, timeout = 5 seconds, "tutorQueue")

  private val durationCache = cacheApi.unit[FiniteDuration] {
    _.refreshAfterWrite(1 minutes)
      .buildAsyncFuture { _ =>
        reportColl
          .aggregateOne(ReadPreference.secondaryPreferred) { framework =>
            import framework._
            Sort(Descending(TutorFullReport.F.at)) -> List(
              Limit(100),
              Group(BSONNull)(TutorFullReport.F.millis -> AvgField(TutorFullReport.F.millis))
            )
          }
          .map {
            ~_.flatMap(_.getAsOpt[Int](TutorFullReport.F.millis))
          }
          .map(_.millis)
      }
  }

  def status(user: User): Fu[Status] = workQueue { fetchStatus(user) }

  def enqueue(user: User): Fu[Status] = workQueue {
    queueColl.insert
      .one($doc(F.id -> user.id, F.requestedAt -> DateTime.now))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(user)
  }

  implicit private val nextReader    = Macros.reader[Next]
  def next: Fu[Option[Next]]         = queueColl.find($empty).sort($sort asc F.requestedAt).one[Next]
  def start(userId: User.ID): Funit  = queueColl.updateField($id(userId), F.startedAt, DateTime.now).void
  def remove(userId: User.ID): Funit = queueColl.delete.one($id(userId)).void

  private def fetchStatus(user: User): Fu[Status] =
    queueColl.primitiveOne[DateTime]($id(user.id), F.requestedAt) flatMap {
      case None => fuccess(NotInQueue)
      case Some(at) =>
        for {
          position    <- queueColl.countSel($doc(F.requestedAt $lte at))
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

  private[tutor] case class Next(_id: User.ID, startedAt: Option[DateTime]) {
    def userId = _id
  }

  object F {
    val id          = "_id"
    val requestedAt = "requestedAt"
    val startedAt   = "startedAt"
  }
}
