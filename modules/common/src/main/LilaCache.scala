package lila.common

import com.github.benmanes.caffeine.cache.{ Caffeine, Scheduler }
import com.github.blemale.scaffeine.Scaffeine
import java.util.concurrent.Executor
import play.api.Mode

object LilaCache {

  def scaffeine: Scaffeine[Any, Any] = Scaffeine().scheduler(Scheduler.systemScheduler)

  private val singleThreadExecutor = new Executor {
    def execute(r: Runnable): Unit = r.run()
  }

  def scaffeineSameThreadExecutor: Scaffeine[Any, Any] =
    Scaffeine().executor(singleThreadExecutor).scheduler(Scheduler.systemScheduler)
}
