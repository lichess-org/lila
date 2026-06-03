package lila.common

import scalalib.actor.{ AsyncActorBounded, AsyncActorSequencer }

// provides values of A one by one
// but generates them in batches
final class BatchProvider[A](name: String, timeout: FiniteDuration, monitor: AsyncActorBounded.Monitor)(
    generateBatch: () => Fu[List[A]]
)(using
    Executor,
    Scheduler
):

  private val workQueue = scalalib.actor.AsyncActorSequencer(
    maxSize = Max(4096),
    timeout = timeout,
    name = s"$name.batchProvider.workQueue",
    monitor = monitor
  )

  private var reserve = List.empty[A]

  def one: Fu[A] = workQueue:
    reserve.match
      case head :: tail =>
        reserve = tail
        fuccess(head)
      case Nil =>
        generateBatch().map:
          case head :: tail =>
            reserve = tail
            head
          case Nil => sys.error(s"$name: couldn't generate batch")
