package lila.tutor

import lila.common.{ Chronometer, LilaScheduler, Uptime }
import lila.core.perf.UserWithPerfs
import lila.db.dsl.{ *, given }
import lila.memo.CacheApi

final class TutorApi(
    colls: TutorColls,
    queue: TutorQueue,
    builder: TutorBuilder,
    cacheApi: CacheApi
)(using Executor, Scheduler)(using mode: play.api.Mode):

  import TutorBsonHandlers.given

  def availability(user: UserWithPerfs): Fu[TutorFullReport.Availability] =
    cache
      .get(user.id)
      .flatMap:
        case Some(report) if report.isFresh => fuccess(TutorFullReport.Available(report, none))
        case Some(report) => queue.status(user).dmap(some).map { TutorFullReport.Available(report, _) }
        case None =>
          builder.eligiblePerfKeysOf(user) match
            case Nil => fuccess(TutorFullReport.InsufficientGames)
            case _ => queue.status(user).map(TutorFullReport.Empty.apply)

  def request(user: User, availability: TutorFullReport.Availability): Fu[TutorFullReport.Availability] =
    availability match
      case TutorFullReport.Empty(TutorQueue.NotInQueue) =>
        queue.enqueue(user).dmap(TutorFullReport.Empty.apply)
      case TutorFullReport.Available(report, Some(TutorQueue.NotInQueue)) =>
        queue.enqueue(user).dmap(some).map { TutorFullReport.Available(report, _) }
      case availability => fuccess(availability)

  private val initialDelay = if mode.isProd then 1.minute else 5.seconds
  LilaScheduler("TutorApi", _.Every(1.second), _.AtMost(10.seconds), _.Delay(initialDelay))(pollQueue)

  private def pollQueue = queue.next.flatMap: items =>
    lila.mon.tutor.parallelism.update(items.size)
    items.sequentiallyVoid: next =>
      next.startedAt.fold(buildThenRemoveFromQueue(next.userId)) { started =>
        val expired =
          started.isBefore(nowInstant.minusSeconds(builder.maxTime.toSeconds.toInt)) ||
            started.isBefore(Uptime.startedAt)
        for _ <- expired.so(queue.remove(next.userId)) yield lila.mon.tutor.buildTimeout.increment()
      }

  // we only wait for queue.start
  // NOT for builder
  private def buildThenRemoveFromQueue(userId: UserId) =
    val chrono = Chronometer.start
    logger.info(s"Start $userId")
    for _ <- queue.start(userId)
    yield builder(userId).foreach: built =>
      logger.info:
        s"${if built.isDefined then "Complete" else "Fail"} $userId in ${chrono().seconds} seconds"
      cache.put(
        userId,
        built match
          case Some(report) => fuccess(report.some)
          case None => findLatest(userId)
      )
      queue.remove(userId)

  private val cache = cacheApi[UserId, Option[TutorFullReport]](256, "tutor.report"):
    _.expireAfterAccess(if mode.isProd then 2 minutes else 1 second)
      .maximumSize(1024)
      .buildAsyncFuture(findLatest)

  private def findLatest(userId: UserId) = colls.report
    .find($doc(TutorFullReport.F.user -> userId))
    .sort($sort.desc(TutorFullReport.F.at))
    .one[TutorFullReport]
