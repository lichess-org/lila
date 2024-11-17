package lila.recap

import com.softwaremill.tagging.*
import lila.memo.SettingStore
import lila.db.dsl.*
import akka.stream.scaladsl.*

/* Enqueue heavy computations
 * - stores the queue in mongodb, allowing unbounded queue size and resiliency
 * - allows parallel processing of the queue with dynamic worker size
 */
final private class ParallelMongoQueue[A, B](
    coll: Coll,
    parallelism: () => Int,
    computationTimeout: FiniteDuration,
    name: String
)(computation: A => Fu[B])(using Executor):

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 5.seconds,
    s"name.workQueue",
    lila.log.asyncActorMonitor.full
  )

  def enqueue(a: A): Fu[B] = ???

  def enqueue(user: User): Fu[Status] = workQueue:
    colls.queue.insert
      .one($doc(F.id -> user.id, F.requestedAt -> nowInstant))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(user)
