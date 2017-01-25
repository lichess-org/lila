package lila.memo

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._

import com.google.common.cache.{ LoadingCache => SyncCache }

/**
 * A synchronous cache from asynchronous computations.
 * It will attempt to serve cached responses synchronously.
 * If none is available, it starts an async computation,
 * and either waits for the result or serves a default value.
 */
final class Syncache[K, V](
    name: String,
    compute: K => Fu[V],
    default: K => V,
    strategy: Syncache.Strategy,
    timeToLive: FiniteDuration,
    logger: lila.log.Logger,
    resultTimeout: FiniteDuration = 5 seconds)(implicit system: akka.actor.ActorSystem) {

  import Syncache._

  // ongoing async computations
  private val chm = new ConcurrentHashMap[K, Fu[V]]

  // sync cached values
  private val sync = Builder.expiry[K, V](timeToLive)

  def get(k: K): V = Option(sync getIfPresent k) getOrElse {
    // println(s"*** $name miss $k")
    chm.computeIfAbsent(k, loadFunction)
    strategy match {
      case NeverWait            => default(k)
      case AlwaysWait(duration) => waitForResult(k, duration)
      case WaitAfterUptime(duration, uptime) =>
        if (lila.common.PlayApp startedSinceSeconds uptime) waitForResult(k, duration)
        else default(k)
    }
  }

  def invalidate(k: K): Unit = sync invalidate k

  // TODO preload stuff (homepage usernames)
  // def preload(keys: List[K]): Funit

  private val loadFunction = new java.util.function.Function[K, Fu[V]] {
    def apply(k: K) = {
      // println(s"*** $name chm put $k")
      compute(k).withTimeout(
        duration = resultTimeout,
        error = lila.common.LilaException(s"Syncache $name $k timed out after $resultTimeout")
      ).addEffects(
          err => {
            // println(s"*** $name chm fail $k")
            logger.branch(name).warn(s"$err key=$k")
            chm remove k
          },
          res => {
            // println(s"*** $name sync put $k")
            sync.put(k, res)
            chm remove k
          }
        )
    }
  }

  private def waitForResult(k: K, duration: FiniteDuration): V =
    Option(chm get k).fold(default(k)) { fu =>
      try {
        val v = fu await duration
        // println(s"*** $name wait success $k")
        v
      }
      catch {
        case e: java.util.concurrent.TimeoutException =>
          // println(s"*** $name wait timeout $k $e")
          default(k)
      }
    }
}

object Syncache {

  sealed trait Strategy

  case object NeverWait extends Strategy
  case class AlwaysWait(duration: FiniteDuration) extends Strategy
  case class WaitAfterUptime(duration: FiniteDuration, uptimeSeconds: Int = 12) extends Strategy
}
