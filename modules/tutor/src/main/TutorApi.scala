package lila.tutor

import cats.syntax.all.*
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

  def availability(user: User): Fu[TutorFullReport.Availability] =
    cache.get(user.id) flatMap {
      case Some(report) if report.isFresh => fuccess(TutorFullReport.Available(report, none))
      case Some(report) => queue.status(user) dmap some map { TutorFullReport.Available(report, _) }
      case None =>
        builder.eligiblePerfTypesOf(user) match
          case Nil => fuccess(TutorFullReport.InsufficientGames)
          case _   => queue.status(user) map TutorFullReport.Empty.apply
    }

  def request(user: User, availability: TutorFullReport.Availability): Fu[TutorFullReport.Availability] =
    availability match
      case TutorFullReport.Empty(TutorQueue.NotInQueue) =>
        queue.enqueue(user) dmap TutorFullReport.Empty.apply
      case TutorFullReport.Available(report, Some(TutorQueue.NotInQueue)) =>
        queue.enqueue(user) dmap some map { TutorFullReport.Available(report, _) }
      case availability => fuccess(availability)

  LilaScheduler("TutorApi", _.Every(1 second), _.AtMost(10 seconds), _.Delay(3 seconds))(pollQueue)

  private def pollQueue = queue.next.flatMap: items =>
    lila.mon.tutor.parallelism.update(items.size)
    items.traverse_ : next =>
      next.startedAt.fold(buildThenRemoveFromQueue(next.userId)) { started =>
        val expired =
          started.isBefore(nowInstant minusSeconds builder.maxTime.toSeconds.toInt) ||
            started.isBefore(Uptime.startedAt)
        expired so queue.remove(next.userId) >>- lila.mon.tutor.buildTimeout.increment().unit
      }

  // we only wait for queue.start
  // NOT for builder
  private def buildThenRemoveFromQueue(userId: UserId) =
    val chrono = Chronometer.start
    logger.info(s"Start $userId")
    queue.start(userId) >>- {
      builder(userId) foreach { built =>
        logger.info(
          s"${if built.isDefined then "Complete" else "Fail"} $userId in ${chrono().seconds} seconds"
        )
        built match
          case Some(report) => cache.put(userId, fuccess(report.some))
          case None         => cache.put(userId, findLatest(userId))
        queue.remove(userId)
      }
    }

  private val cache = cacheApi[UserId, Option[TutorFullReport]](256, "tutor.report") {
    // _.expireAfterAccess(if (mode == Mode.Prod) 5 minutes else 1 second)
    _.expireAfterAccess(3.minutes)
      .maximumSize(1024)
      .buildAsyncFuture(findLatest)
  }

  private def findLatest(userId: UserId) = colls.report
    .find($doc(TutorFullReport.F.user -> userId))
    .sort($sort desc TutorFullReport.F.at)
    .one[TutorFullReport]
