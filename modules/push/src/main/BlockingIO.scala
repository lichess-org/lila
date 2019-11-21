package lila.push

import java.util.concurrent.{ Executors, ExecutorService }
import scala.concurrent.{ Promise, Future, blocking }
import scala.util.control.NonFatal

object BlockingIO {
  private val threadPool: ExecutorService =
    Executors.newFixedThreadPool(100)

  def execute[T](cb: => T): Future[T] = {
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
