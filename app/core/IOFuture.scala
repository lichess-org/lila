package lila
package core

import play.api.Play.current
import play.api.libs.concurrent._
import akka.dispatch.{ Future, Await }
import akka.util.Timeout
import akka.util.Duration
import akka.util.duration._
import scalaz.effects._

trait Futuristic {

  protected implicit val ttl = 1 minute
  protected implicit val executor = Akka.system.dispatcher

  implicit def ioToFuture[A](ioa: IO[A]) = new {

    def toFuture: Future[A] = Future(ioa.unsafePerformIO)
  }

  implicit def futureToIo[A](fa: Future[A]) = new {

    def toIo: IO[A] = toIo(1 minute)

    def toIo(duration: Duration): IO[A] = io {
      Await.result(fa, duration)
    }
  }
}
