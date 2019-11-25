package lila.push

import java.util.concurrent.{ Executors, SynchronousQueue, ThreadPoolExecutor, ThreadFactory, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ Promise, Future, blocking }
import scala.util.control.NonFatal

private final class BlockingIO(
    minNbThread: Integer,
    maxNbThread: Integer
) {
  private def cpus = Runtime.getRuntime.availableProcessors

  /**
   * Current google oauth client (v0.18.0) doesn't support timeout setting
   * (default is 60s).
   * To avoid any problem, let's use bounded thread pool with direct handoff,
   * ie. no queuing.
   * Uses the default AbortPolicy, so if there is no more thread available
   * it will reject the task and throw a RejectedExecutionException
   */
  private val threadPool = new ThreadPoolExecutor(
    minNbThread * cpus,
    maxNbThread * cpus,
    60,
    TimeUnit.SECONDS,
    new SynchronousQueue[Runnable](),
    new ThreadFactory {
      private val count = new AtomicInteger()

      override def newThread(r: Runnable) = {
        val thread = new Thread(r)
        thread.setName(s"push-blocking-io-thread-${count.incrementAndGet}")
        thread.setDaemon(true)
        thread
      }
    }
  )

  def apply[T](cb: => T): Future[T] = {
    val p = Promise[T]()

    threadPool.execute(new Runnable {
      def run() = try {
        p.success(blocking(cb))
      } catch {
        case NonFatal(ex) => p.failure(ex)
      }
    })

    p.future
  }
}
