package lila.common

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.github.blemale.scaffeine.Scaffeine

object LilaCache {

  def caffeine: Caffeine[Any, Any] =
    Caffeine.newBuilder().scheduler(Scheduler.systemScheduler)

  def scaffeine: Scaffeine[Any, Any] =
    Scaffeine().scheduler(Scheduler.systemScheduler)
}
