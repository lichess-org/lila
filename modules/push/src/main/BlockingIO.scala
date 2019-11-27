package lila.push

import java.util.concurrent.{ Executors, SynchronousQueue, ThreadPoolExecutor, ThreadFactory, TimeUnit }
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.{ Promise, Future }
import scala.util.control.NonFatal

private object BlockingIO {

  /**
   * Current google oauth client (v0.18.0) doesn't support timeout setting
   * (default is 60s).
   * To avoid any problem, let's use a thread pool with direct handoff (no
   * queuing), and bounded at the number of processors, so it can handle a few
   * concurrent requests.
   * Uses the default AbortPolicy, so if there is no more thread available
   * it will reject the task and throw a RejectedExecutionException
   */
  private val threadPool = new ThreadPoolExecutor(
    1, // min threads
    4, // max threads
    60, //keep alive (?)
    TimeUnit.SECONDS,
    new SynchronousQueue[Runnable](),
    new ThreadFactory {
      private val count = new AtomicInteger()

      override def newThread(r: Runnable) = {
        val threadName = s"push-blocking-io-thread-${count.incrementAndGet}"
        logger.info(s"Create blocking IO thread $threadName")
        val thread = new Thread(r)
        thread.setName(threadName)
        thread.setDaemon(true)
        thread
      }
    }
  )

  def apply[T](cb: => T): Fu[T] = {
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
