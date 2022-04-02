package lila.common

import play.api.Mode
import com.github.benmanes.caffeine.cache.{ Caffeine, Scheduler }
import com.github.blemale.scaffeine.Scaffeine

object LilaCache {

  def caffeine(mode: Mode): Caffeine[Any, Any] = Caffeine.newBuilder().scheduler(Scheduler.systemScheduler)

  def scaffeine(mode: Mode): Scaffeine[Any, Any] = Scaffeine().scheduler(Scheduler.systemScheduler)
}
