package lila.tutor

import com.softwaremill.tagging._
import scala.concurrent.ExecutionContext

import lila.db.dsl._
import lila.user.User

final class TutorApi(queue: TutorQueue, builder: TutorBuilder, reportColl: Coll @@ ReportColl)(implicit
    ec: ExecutionContext
) {

  import TutorBsonHandlers._

  def request(user: User): Fu[TutorReport.Availability] =
    latest(user) flatMap {
      case TutorReport.Empty(TutorQueue.NotInQueue) => queue.enqueue(user) map TutorReport.Empty
      case TutorReport.Stale(report, TutorQueue.NotInQueue) =>
        queue.enqueue(user) map { TutorReport.Stale(report, _) }
      case availability => fuccess(availability)
    }

  def latest(user: User): Fu[TutorReport.Availability] =
    reportColl
      .find($doc(TutorReport.F.user -> user.id))
      .sort($sort desc TutorReport.F.at)
      .one[TutorReport] flatMap {
      case Some(report) if report.isFresh => fuccess(TutorReport.Fresh(report))
      case Some(report)                   => queue.status(user) map { TutorReport.Stale(report, _) }
      case None =>
        builder.tutorablePerfTypesOf(user) match {
          case Nil => fuccess(TutorReport.InsufficientGames)
          case _   => queue.status(user) map TutorReport.Empty
        }
    }
}
