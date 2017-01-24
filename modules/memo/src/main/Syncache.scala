package lila.memo

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._

import com.google.common.cache.{ LoadingCache => SyncCache }

final class Syncache[K, V](
    name: String,
    compute: K => Fu[V],
    default: K => V,
    timeToLive: FiniteDuration,
    awaitTime: FiniteDuration,
    resultTimeout: FiniteDuration = 5 seconds,
    logger: lila.log.Logger)(implicit system: akka.actor.ActorSystem) {

  private val chm = new ConcurrentHashMap[K, Fu[V]]

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
      default(k)
  }

  def invalidate(k: K): Unit = sync invalidate k
}
