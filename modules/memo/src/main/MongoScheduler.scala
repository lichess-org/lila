package lila.memo

import lila.db.dsl.*
import lila.core.config.*
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import lila.common.LilaScheduler
import akka.actor.Cancellable
import play.api.Mode

final class MongoSchedulerApi(db: lila.db.Db)(using Executor, Scheduler, Mode):

  def make[Data: BSONHandler](key: String)(computation: Data => Funit): MongoScheduler[Data] =
    MongoScheduler(db(CollName(s"mongo_scheduler_$key")), key)(computation)

object MongoScheduler:

  case class Entry[Data](data: Data, runAfter: Instant)
  object F:
    val data     = "data"
    val runAfter = "runAfter"

/* Schedules computations in a MongoDB collection
 * persists the queue to survive restarts
 * the granularity is 10.seconds so it's not suitable for high frequency tasks
 */
final class MongoScheduler[Data: BSONHandler](
    coll: Coll,
    name: String
)(computation: Data => Funit)(using Executor)(using scheduler: Scheduler, mode: Mode):

  import MongoScheduler.*

  private type E = Entry[Data]

  private given BSONDocumentHandler[E] = Macros.handler

  def schedule(data: Data, delay: FiniteDuration): Funit =
    coll.insert.one($doc(F.data -> data, F.runAfter -> nowInstant.plusMillis(delay.toMillis))).void

  private val startAfter = if mode.isProd then 28.seconds else 1.seconds

  scheduler.scheduleOnce(startAfter)(lookupAndRun())

  private def lookupAndRun(): Unit =
    popNext()
      .map:
        case Some(data) =>
          computation(data)
            .withTimeout(30.seconds, s"MongoScheduler $name")
            .addEffectAnyway:
              lookupAndRunIn(1.second)
        case None => lookupAndRunIn(10.seconds)
      .addFailureEffect: _ =>
        lookupAndRunIn(10.seconds)

  private var nextLookup = none[Cancellable]
  private def lookupAndRunIn(delay: FiniteDuration): Unit =
    nextLookup.foreach(_.cancel())
    nextLookup = scheduler.scheduleOnce(delay)(lookupAndRun()).some

  private def popNext(): Fu[Option[Data]] =
    coll
      .findAndRemove(
        selector = $doc(F.runAfter.$gte(nowInstant)),
        sort = $sort.asc(F.runAfter).some
      )
      .map:
        _.result[Bdoc].flatMap(_.getAsOpt[Data](F.data))
