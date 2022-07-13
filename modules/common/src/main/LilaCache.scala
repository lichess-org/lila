package lila.common

import com.github.benmanes.caffeine.cache.{ Caffeine, Scheduler }
import com.github.blemale.scaffeine.Scaffeine
import java.util.concurrent.Executor
import play.api.Mode

object LilaCache {

  def scaffeine: Scaffeine[Any, Any] = Scaffeine().scheduler(Scheduler.systemScheduler)
}
