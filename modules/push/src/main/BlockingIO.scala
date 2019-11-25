package lila.push

import java.util.concurrent.{ Executors, SynchronousQueue, ThreadPoolExecutor, ThreadFactory, TimeUnit }
import scala.concurrent.{ Promise, Future }
import scala.util.control.NonFatal

object BlockingIO {
  /**
   * Current google oauth client (v0.18.0) doesn't support timeout setting
   * (default is 60s).
   * To avoid any problem, let's use a bounded thread pool with direct handoff,
   * ie. no queuing.
   * Uses the default AbortPolicy, so if there is no more thread available
   * it will reject the task and throw a RejectedExecutionException
   */
  private val threadPool = new ThreadPoolExecutor(
    1,
    1,
    60,
    TimeUnit.SECONDS,
    new SynchronousQueue[Runnable](),
    new ThreadFactory {
      override def newThread(r: Runnable) = {
        val thread = new Thread(r)
        thread.setName("push-blocking-io-thread")
        thread.setDaemon(true)
        thread
      }
    }
  )

  def apply[T](cb: => T): Future[T] = {
    val p = Promise[T]()

    threadPool.execute(new Runnable {
      def run() = try {
        p.success(cb)
      } catch {
        case NonFatal(ex) => p.failure(ex)
      }
    })

    p.future
  }
}
