package lila.memo

import lila.db.dsl.*
import reactivemongo.api.bson.*
import reactivemongo.api.bson.Macros.Annotations.Key
import lila.common.Uptime
import lila.common.LilaScheduler

/* Enqueue heavy computations
 * - persists the queue, allowing unbounded queue size and resiliency
 * - allows parallel processing of the queue with dynamic worker size
 */
trait ParallelQueue[A]:
  def enqueue(a: A): Fu[ParallelQueue.Entry[A]]
  def status(a: A): Fu[Option[ParallelQueue.Entry[A]]]

object ParallelQueue:

  case class Entry[A](@Key("_id") id: A, createdAt: Instant, startedAt: Option[Instant])
  object F:
    val id        = "_id"
    val createdAt = "createdAt"
    val startedAt = "startedAt"

final class ParallelMongoQueue[A: BSONHandler](
    coll: Coll,
    parallelism: () => Int,
    computationTimeout: FiniteDuration,
    name: String
)(computation: A => Funit)(using Executor, Scheduler)(using mode: play.api.Mode)
    extends ParallelQueue[A]:

  import ParallelQueue.*
  private given BSONDocumentHandler[Entry[A]] = Macros.handler[Entry[A]]

  def enqueue(a: A): Fu[Entry[A]] = workQueue:
    status(a).flatMap:
      case Some(entry) => fuccess(entry)
      case None =>
        val entry = Entry(a, nowInstant, none)
        for _ <- coll.insert.one(entry).recover(lila.db.ignoreDuplicateKey)
        yield entry

  def status(a: A): Fu[Option[Entry[A]]] = coll.byId[Entry[A]](a)

  private val monitoring = lila.mon.parallelQueue(name)

  // just to prevent race conditions when enqueuing stuff
  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(256),
    timeout = 5.seconds,
    s"$name.workQueue",
    lila.log.asyncActorMonitor.full
  )

  /* Read the oldest <parallelism()> entries from the queue
   * start new ones, expire old ones
   */
  private val startAfter = if mode.isProd then 33.seconds else 3.seconds
  LilaScheduler(s"ParallelQueue($name).poll", _.Every(1.second), _.AtMost(5.seconds), _.Delay(startAfter)):

    def fetchEntriesToProcess: Fu[List[Entry[A]]] =
      coll.find($empty).sort($sort.asc(F.createdAt)).cursor[Entry[A]]().list(parallelism())

    def start(id: A): Funit = coll.updateField($id(id), F.startedAt, nowInstant).void

    // we only wait for enqueuing - NOT for the computation
    def computeThenRemoveFromQueue(id: A): Funit =
      for _ <- start(id)
      yield computation(id).foreach: _ =>
        remove(id)

    def remove(id: A): Funit = coll.delete.one($id(id)).void

    fetchEntriesToProcess.flatMap: entries =>
      monitoring.parallelism.update(entries.size)
      entries.sequentiallyVoid: next =>
        next.startedAt.fold(computeThenRemoveFromQueue(next.id)): started =>
          val expired =
            started.isBefore(nowInstant.minusSeconds(computationTimeout.toSeconds.toInt)) ||
              started.isBefore(Uptime.startedAt)
          expired.so:
            for
              _ <- remove(next.id)
              _ = monitoring.computeTimeout.increment()
            yield ()
