package lila.common

import play.api.Mode
import com.github.benmanes.caffeine.cache.{ Caffeine, Scheduler }
import com.github.blemale.scaffeine.Scaffeine

object LilaCache {

  def caffeine(mode: Mode): Caffeine[Any, Any] =
    if (mode == Mode.Prod) Caffeine.newBuilder().scheduler(Scheduler.systemScheduler)
    else Caffeine.newBuilder() // systemScheduler causes play reload classloader leaks :-/

  def scaffeine(mode: Mode): Scaffeine[Any, Any] =
    if (mode == Mode.Prod) Scaffeine().scheduler(Scheduler.systemScheduler)
    else Scaffeine() // systemScheduler causes play reload classloader leaks :-/
}
