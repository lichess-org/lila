package lila.memo

import com.softwaremill.tagging.*
import lila.memo.SettingStore
import lila.db.dsl.*
import akka.stream.scaladsl.*
import reactivemongo.api.bson.*
import lila.common.Uptime
import lila.common.LilaScheduler

/* Enqueue heavy computations
 * - stores the queue in mongodb, allowing unbounded queue size and resiliency
 * - allows parallel processing of the queue with dynamic worker size
 */
trait ParallelQueue[A]:
  def enqueue(a: A): Fu[ParallelQueue.Status]
  def status(a: A): Fu[ParallelQueue.Status]

object ParallelQueue:
  enum Status:
    case NotInQueue             extends Status
    case InQueue(position: Int) extends Status

  case class Entry[A](id: A, requestedAt: Instant, startedAt: Option[Instant])
  object F:
    val id          = "_id"
    val requestedAt = "requestedAt"
    val startedAt   = "startedAt"

final class ParallelMongoQueue[A: BSONHandler](
    coll: Coll,
    parallelism: () => Int,
    computationTimeout: FiniteDuration,
    name: String
)(computation: A => Funit)(using Executor, Scheduler)
    extends ParallelQueue[A]:

  import ParallelQueue.*
  private given BSONDocumentHandler[Entry[A]] = Macros.handler[Entry[A]]

  def enqueue(a: A): Fu[Status] = workQueue:
    coll.insert
      .one($doc(F.id -> a, F.requestedAt -> nowInstant))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(a)

  def status(a: A): Fu[Status] = fetchStatus(a)

  private val monitoring = lila.mon.parallelQueue(name)

  // just to prevent race conditions when enqueuing stuff
  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 5.seconds,
    s"$name.workQueue",
    lila.log.asyncActorMonitor.full
  )

  /* Read the oldest <parallelism()> entries from the queue
   * start new ones, expire old ones
   */
  // LilaScheduler(s"ParallelQueue($name).poll", _.Every(1 second), _.AtMost(5 seconds), _.Delay(33 seconds)):
  LilaScheduler(s"ParallelQueue($name).poll", _.Every(1 second), _.AtMost(5 seconds), _.Delay(3 seconds)):

    def fetchEntriesToProcess: Fu[List[Entry[A]]] =
      coll.find($empty).sort($sort.asc(F.requestedAt)).cursor[Entry[A]]().list(parallelism())

    def start(id: A): Funit = coll.updateField($id(id), F.startedAt, nowInstant).void

    // we only wait for enqueuing - NOT for the computation
    def computeThenRemoveFromQueue(id: A): Funit =
      for _ <- start(id)
      yield computation(id).foreach: done =>
        remove(id)

    def remove(id: A): Funit = coll.delete.one($id(id)).void

    fetchEntriesToProcess.flatMap: entries =>
      monitoring.parallelism.update(entries.size)
      entries.sequentiallyVoid: next =>
        next.startedAt.fold(computeThenRemoveFromQueue(next.id)): started =>
          val expired =
            started.isBefore(nowInstant.minusSeconds(computationTimeout.toSeconds.toInt)) ||
              started.isBefore(Uptime.startedAt)
          for _ <- expired.so(remove(next.id))
          yield monitoring.computeTimeout.increment()

  private def fetchStatus(a: A): Fu[Status] =
    coll.primitiveOne[Instant]($id(a), F.requestedAt).flatMap {
      _.fold(fuccess(Status.NotInQueue)): at =>
        coll
          .countSel($doc(F.requestedAt.$lte(at)))
          .map: position =>
            Status.InQueue(position)
    }
