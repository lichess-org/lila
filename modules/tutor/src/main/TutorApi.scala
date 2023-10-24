package lila.tutor

import lila.common.{ LilaScheduler, Uptime }
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi
import lila.user.User
import lila.common.LilaFuture
import lila.common.Chronometer

final class TutorApi(
    colls: TutorColls,
    queue: TutorQueue,
    builder: TutorBuilder,
    cacheApi: CacheApi
)(using Executor, Scheduler):

  import TutorBsonHandlers.given

  def get(user: User, id: TutorPeriodReport.Id): Fu[Option[TutorPeriodReport]] =
    colls.report.find($id(id) ++ $doc(TutorPeriodReport.F.user -> user.id)).one[TutorPeriodReport]

  def reports(user: User): Fu[TutorPeriodReport.UserReports] = for
    past <- latestReports(user)
    next <- queue.fetchStatus(user)
  yield TutorPeriodReport.UserReports(user, past, next)

  private def latestReports(user: User): Fu[List[TutorPeriodReport.Preview]] =
    colls.report
      .find($doc(TutorPeriodReport.F.user -> user.id))
      .sort($sort desc TutorPeriodReport.F.at)
      .cursor[TutorPeriodReport.Preview]()
      .list(30)

  LilaScheduler("TutorApi", _.Every(1 second), _.AtMost(10 seconds), _.Delay(3 seconds))(pollQueue)

  private def pollQueue = queue.next.flatMap: items =>
    lila.mon.tutor.parallelism.update(items.size)
    items.traverse_ : next =>
      next.startedAt.fold(buildThenRemoveFromQueue(next)): started =>
        val expired =
          started.isBefore(nowInstant minusSeconds builder.maxTime.toSeconds.toInt) ||
            started.isBefore(Uptime.startedAt)
        expired so queue.remove(next.userId) andDo lila.mon.tutor.buildTimeout.increment()

  // we only wait for queue.start
  // NOT for builder
  private def buildThenRemoveFromQueue(next: TutorQueue.Queued) =
    val chrono = Chronometer.start
    logger.info(s"Start $next")
    queue.start(next.userId) andDo {
      builder(next.query).foreach: built =>
        logger.info:
          s"${if built.isDefined then "Complete" else "Fail"} $next in ${chrono().seconds} seconds"
        queue.remove(next.userId)
    }
