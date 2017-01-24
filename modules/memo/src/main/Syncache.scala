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

  private val loadFunction = new java.util.function.Function[K, Fu[V]] {
    def apply(k: K) = {
      println(s"*** $name chm put $k")
      compute(k).withTimeout(
        duration = resultTimeout,
        error = lila.common.LilaException(s"MixedCache2 $name $k timed out after $resultTimeout")
      ).addEffects(
          err => {
            println(s"*** $name chm fail $k")
            logger.branch(name).warn(s"$err key=$k")
            chm remove k
          },
          res => {
            println(s"*** $name sync put $k")
            sync.put(k, res)
            chm remove k
          }
        )
    }
  }

  def get(k: K): V = Option(sync getIfPresent k) match {
    case Some(v) =>
      println(s"*** $name hit $k")
      v
    case None =>
      println(s"*** $name miss $k")
      chm.computeIfAbsent(k, loadFunction)
      strategy match {
        case NeverWait => default(k)
        case AlwaysWait(duration) => try {
          chm.get(k) await duration
        }
        catch {
          case e: java.util.concurrent.TimeoutException =>
            println(s"*** $name wait $k $e")
            default(k)
        }
      }
  }

  def invalidate(k: K): Unit = sync invalidate k
}

object Syncache {

  sealed trait Strategy

  case object NeverWait extends Strategy
  case class AlwaysWait(duration: FiniteDuration) extends Strategy
}
