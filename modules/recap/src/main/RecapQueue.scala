package lila.recap

import com.softwaremill.tagging.*
import lila.memo.SettingStore
import lila.db.dsl.*
import akka.stream.scaladsl.*
import reactivemongo.api.bson.BSONHandler

/* Enqueue heavy computations
 * - stores the queue in mongodb, allowing unbounded queue size and resiliency
 * - allows parallel processing of the queue with dynamic worker size
 */
trait ParallelQueue[A, B]:
  def enqueue(a: A): Fu[B]
  def status(a: A): Fu[ParallelQueue.Status]

object ParallelQueue:
  enum Status:
    case NotInQueue             extends Status
    case InQueue(position: Int) extends Status

final private class ParallelMongoQueue[A: BSONHandler, B](
    coll: Coll,
    parallelism: () => Int,
    computationTimeout: FiniteDuration,
    name: String
)(computation: A => Fu[B])(using Executor)
    extends ParallelQueue[A, B]:

  import ParallelQueue.*

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(64),
    timeout = 5.seconds,
    s"$name.workQueue",
    lila.log.asyncActorMonitor.full
  )

  def enqueue(a: A): Fu[B] = workQueue:
    colls.queue.insert
      .one($doc("_id" -> a, "requestedAt" -> nowInstant))
      .recover(lila.db.ignoreDuplicateKey)
      .void >> fetchStatus(user)

  private def fetchStatus(a: A): Fu[Status] =
    coll.primitiveOne[Instant]($id(a), "requestedAt").flatMap {
      _.fold(fuccess(Status.NotInQueue)): at =>
        coll
          .countSel($doc("requestedAt".$lte(at)))
          .map: position =>
            Status.InQueue(position)
    }
