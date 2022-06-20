package lila.tutor

import com.softwaremill.tagging._
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.common.{ AtMost, Every, ResilientScheduler, Uptime }
import lila.db.dsl._
import lila.user.User

final class TutorApi(queue: TutorQueue, builder: TutorBuilder, reportColl: Coll @@ ReportColl)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

  import TutorBsonHandlers._

  def availability(user: User): Fu[TutorReport.Availability] =
    builder findLatest user flatMap {
      case Some(report) if report.isFresh => fuccess(TutorReport.Available(report, none))
      case Some(report) => queue.status(user) dmap some map { TutorReport.Available(report, _) }
      case None =>
        builder.tutorablePerfTypesOf(user) match {
          case Nil => fuccess(TutorReport.InsufficientGames)
          case _   => queue.status(user) map TutorReport.Empty
        }
    }

  def request(user: User): Fu[TutorReport.Availability] = availability(user) flatMap { request(user, _) }
  def request(user: User, availability: TutorReport.Availability): Fu[TutorReport.Availability] =
    availability match {
      case TutorReport.Empty(TutorQueue.NotInQueue) => queue.enqueue(user) map TutorReport.Empty
      case TutorReport.Available(report, Some(TutorQueue.NotInQueue)) =>
        queue.enqueue(user) dmap some map { TutorReport.Available(report, _) }
      case availability => fuccess(availability)
    }

  ResilientScheduler(every = Every(1 second), timeout = AtMost(10 seconds), initialDelay = 3 seconds) {
    queue.next flatMap {
      _ ?? { next =>
        next.startedAt match {
          case None => queue.start(next.userId) >> computeThenRemoveFromQueue(next.userId)
          case Some(at) if at.isBefore(DateTime.now minusMinutes 5) || at.isBefore(Uptime.startedAt) =>
            queue.removeAndStartNext(next.userId).map2(_.userId) flatMap { _ ?? computeThenRemoveFromQueue }
          case _ => funit
        }
      }
    }
  }

  private def computeThenRemoveFromQueue(userId: User.ID) =
    builder.compute(userId) >> queue.remove(userId)
}
