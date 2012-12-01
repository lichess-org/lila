package lila
package core

import play.api.Play.current
import play.api.libs.concurrent._
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scalaz.effects._

object Futuristic extends Futuristic

trait Futuristic {

  protected implicit val ttl = 1 minute
  protected implicit val executor = Akka.system.dispatcher

  implicit def ioToFuture[A](ioa: IO[A]) = new {

    def toFuture: Future[A] = Future(ioa.unsafePerformIO)
  }

  implicit def futureToIo[A](fa: Future[A]) = new {

    def toIo: IO[A] = toIo(ttl)

    def toIo(duration: Duration): IO[A] = io {
      Await.result(fa, duration)
    }
  }

  implicit def autoIoToFuture[A](ioa: IO[A]) = Future(ioa.unsafePerformIO)

  implicit def autoFutureToIo[A](fa: Future[A]) = io {
    Await.result(fa, ttl)
  }
}
