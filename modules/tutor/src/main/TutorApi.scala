package lila.tutor

import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.{ LilaScheduler, Uptime }
import lila.db.dsl._
import lila.user.User

final class TutorApi(queue: TutorQueue, builder: TutorBuilder, reportColl: Coll @@ ReportColl)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import TutorBsonHandlers._

  def availability(user: User): Fu[TutorReport.Availability] =
    findLatest(user) flatMap {
      case Some(report) if report.isFresh => fuccess(TutorReport.Available(report, none))
      case Some(report) => queue.status(user) dmap some map { TutorReport.Available(report, _) }
      case None =>
        builder.eligiblePerfTypesOf(user) match {
          case Nil => fuccess(TutorReport.InsufficientGames)
          case _   => queue.status(user) map TutorReport.Empty
        }
    }

  def request(user: User, availability: TutorReport.Availability): Fu[TutorReport.Availability] =
    availability match {
      case TutorReport.Empty(TutorQueue.NotInQueue) => queue.enqueue(user) dmap TutorReport.Empty
      case TutorReport.Available(report, Some(TutorQueue.NotInQueue)) =>
        queue.enqueue(user) dmap some map { TutorReport.Available(report, _) }
      case availability => fuccess(availability)
    }

  LilaScheduler(_.Every(1 second), _.AtMost(10 seconds), _.Delay(3 seconds))(pollQueue)

  private def pollQueue = queue.next flatMap {
    _ ?? { next =>
      next.startedAt match {
        case None => buildThenRemoveFromQueue(next.userId)
        case Some(at) if at.isBefore(DateTime.now minusMinutes 5) || at.isBefore(Uptime.startedAt) =>
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
      builder(userId) >> queue.remove(userId) unit
    }

  private def findLatest(user: User) = reportColl
    .find($doc(TutorReport.F.user -> user.id))
    .sort($sort desc TutorReport.F.at)
    .one[TutorReport]
}
