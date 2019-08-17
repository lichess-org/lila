package lila.memo

import akka.actor.ActorSystem
import scala.concurrent.duration.FiniteDuration

final class PeriodicRefreshCache[A](
    every: FiniteDuration,
    atMost: FiniteDuration,
    f: () => Fu[A],
    default: A,
    logger: lila.log.Logger
)(implicit system: ActorSystem) {

  def get: A = cache

  private var cache: A = default

  system.scheduler.scheduleOnce(every) {
    lila.common.ResilientScheduler(
      every = every,
      atMost = atMost,
      system = system,
      logger = logger
    ) {
      f() map { a =>
        cache = a
      }
    }
  }
}
