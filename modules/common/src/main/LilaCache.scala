package lila.common

import com.github.benmanes.caffeine.cache.{ Caffeine, Scheduler }
import com.github.blemale.scaffeine.Scaffeine
import java.util.concurrent.Executor

object LilaCache:

  def scaffeine: Scaffeine[Any, Any] =
    scaffeineNoScheduler.scheduler(Scheduler.systemScheduler)

  def scaffeineNoScheduler: Scaffeine[Any, Any] =
    Scaffeine().executor(lila.Lila.defaultExecutor)
