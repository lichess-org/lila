package lila.hub

import scala.concurrent.duration._
import scala.concurrent.Promise

import Duct.extra._

final class FutureSequencer(
    executionTimeout: Option[FiniteDuration] = None
)(implicit system: akka.actor.ActorSystem) {

  def apply[A](fu: => Fu[A]): Fu[A] = {
    val promise = Promise[A]()
    duct ! LazyPromise(LazyFu(() => fu), promise)
    promise.future
  }

  def queueSize = duct.queueSize

  private[this] val duct = lazyPromise(executionTimeout)(system)
}
