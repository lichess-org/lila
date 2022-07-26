package lila.tutor

import com.softwaremill.tagging._
import org.joda.time.DateTime
import play.api.Mode
import scala.concurrent.duration._

import lila.common.{ LilaScheduler, Uptime }
import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.User

final class TutorApi(
    queue: TutorQueue,
    builder: TutorBuilder,
    reportColl: Coll @@ ReportColl,
    cacheApi: CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mode: Mode
) {

  import TutorBsonHandlers._

  def availability(user: User): Fu[TutorFullReport.Availability] =
    cache.get(user.id) flatMap {
      case Some(report) if report.isFresh => fuccess(TutorFullReport.Available(report, none))
      case Some(report) => queue.status(user) dmap some map { TutorFullReport.Available(report, _) }
      case None =>
        builder.eligiblePerfTypesOf(user) match {
          case Nil => fuccess(TutorFullReport.InsufficientGames)
          case _   => queue.status(user) map TutorFullReport.Empty
        }
    }

  def request(user: User, availability: TutorFullReport.Availability): Fu[TutorFullReport.Availability] =
    availability match {
      case TutorFullReport.Empty(TutorQueue.NotInQueue) => queue.enqueue(user) dmap TutorFullReport.Empty
      case TutorFullReport.Available(report, Some(TutorQueue.NotInQueue)) =>
        queue.enqueue(user) dmap some map { TutorFullReport.Available(report, _) }
      case availability => fuccess(availability)
    }

  LilaScheduler(_.Every(1 second), _.AtMost(10 seconds), _.Delay(3 seconds))(pollQueue)

  private def pollQueue = queue.next flatMap {
    _ ?? { next =>
      next.startedAt match {
        case None => buildThenRemoveFromQueue(next.userId)
        case Some(at)
            if at.isBefore(DateTime.now minusSeconds builder.maxTime.toSeconds.toInt) || at.isBefore(
              Uptime.startedAt
            ) =>
          for {
            _    <- queue remove next.userId
            next <- queue.next
            _    <- next.map(_.userId) ?? buildThenRemoveFromQueue
          } yield lila.mon.tutor.buildTimeout.increment().unit
        case _ => funit
      }
    }
  }

  // we only wait for queue.start
  // NOT for builder
  private def buildThenRemoveFromQueue(userId: User.ID) =
    queue.start(userId) >>- {
      builder(userId) foreach { reportOpt =>
        cache.put(userId, fuccess(reportOpt))
        queue.remove(userId)
      }
    }

  private val cache = cacheApi[User.ID, Option[TutorFullReport]](256, "tutor.report") {
    _.expireAfterAccess(if (mode == Mode.Prod) 5 minutes else 1 second)
      .maximumSize(1024)
      .buildAsyncFuture(findLatest)
  }

  private def findLatest(userId: User.ID) = reportColl
    .find($doc(TutorFullReport.F.user -> userId))
    .sort($sort desc TutorFullReport.F.at)
    .one[TutorFullReport]
}
