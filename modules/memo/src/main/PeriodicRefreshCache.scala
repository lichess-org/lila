package lidraughts.memo

import akka.actor.ActorSystem
import scala.concurrent.duration._

import lidraughts.common.{ Every, AtMost }

final class PeriodicRefreshCache[A](
    every: Every,
    atMost: AtMost,
    f: () => Fu[A],
    default: A,
    logger: lidraughts.log.Logger,
    initialDelay: FiniteDuration = 1.second
)(implicit system: ActorSystem) {

  def get: A = cache

  private var cache: A = default

  lidraughts.common.ResilientScheduler(every, atMost, logger, initialDelay) {
    f() map { a =>
      cache = a
    }
  }
}
