package lila.memo

import akka.actor.ActorSystem
import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import scala.concurrent.duration._

final class AsyncCache2Single[V] private (cache: AsyncLoadingCache[Unit, V], f: Unit => Fu[V]) {

  def get: Fu[V] = cache.get(())

  def refresh: Unit = cache.put((), f(()))
}

object AsyncCache2Single {

  def apply[V](
    name: String,
    f: => Fu[V],
    expireAfter: ExpireAfter,
    resultTimeout: FiniteDuration = 5 seconds)(implicit system: ActorSystem) = {
    val safeF = (_: Unit) => f.withTimeout(
      duration = resultTimeout,
      error = lila.common.LilaException(s"AsyncCache $name single timed out after $resultTimeout"))
    val b1 = Scaffeine().maximumSize(1)
    val b2 = expireAfter match {
      case ExpireAfterAccess(duration) => b1 expireAfterAccess duration
      case ExpireAfterWrite(duration)  => b1 expireAfterWrite duration
    }
    new AsyncCache2Single[V](
      cache = b2.buildAsyncFuture(safeF),
      safeF)
  }

  sealed trait ExpireAfter
  case class ExpireAfterAccess(duration: FiniteDuration) extends ExpireAfter
  case class ExpireAfterWrite(duration: FiniteDuration) extends ExpireAfter
}
