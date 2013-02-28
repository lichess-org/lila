package lila.app
package core

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import akka.util.Timeout
import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scalaz.effects._
import scalaz.Zero

object Futuristic extends Futuristic

trait Futuristic extends scalaz.Zeros {

  protected implicit val ttl = 1 minute
  protected implicit val executor = Akka.system.dispatcher

  implicit def FuZero[A: Zero]: Zero[Fu[A]] = new Zero[Fu[A]] {
    val zero = Future successful ∅[A]
  }

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

  val funit = Future successful ()
}
