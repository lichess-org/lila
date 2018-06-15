package lila.memo

import com.github.benmanes.caffeine.cache._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

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
    expireAfter: Syncache.ExpireAfter,
    logger: lila.log.Logger,
    resultTimeout: FiniteDuration = 5 seconds
)(implicit system: akka.actor.ActorSystem) {

  import Syncache._

  // ongoing async computations
  private val chm = new ConcurrentHashMap[K, Fu[V]]

  // sync cached values
  private val cache: Cache[K, V] = {
    val b1 = Caffeine.newBuilder().asInstanceOf[Caffeine[K, V]]
    val b2 = expireAfter match {
      case ExpireAfterAccess(duration) => b1.expireAfterAccess(duration.toMillis, TimeUnit.MILLISECONDS)
      case ExpireAfterWrite(duration) => b1.expireAfterWrite(duration.toMillis, TimeUnit.MILLISECONDS)
    }
    val cache = b2.recordStats.build[K, V]()
    AsyncCache.monitor(s"syncache.$name", cache)
    cache
  }

  // get the value synchronously, might block depending on strategy
  def sync(k: K): V = {
    val v = cache getIfPresent k
    if (v != null) v
    else {
      // println(s"*** miss $name $k")
      // Thread.dumpStack()
      incMiss()
      val fu = chm.computeIfAbsent(k, loadFunction)
      strategy match {
        case NeverWait => default(k)
        case AlwaysWait(duration) => waitForResult(k, fu, duration)
        case WaitAfterUptime(duration, uptime) =>
          if (lila.common.PlayApp startedSinceSeconds uptime) waitForResult(k, fu, duration)
          else default(k)
      }
    }
  }

  // get the value asynchronously, never blocks (preferred)
  def async(k: K): Fu[V] = {
    val v = cache getIfPresent k
    if (v != null) fuccess(v)
    else chm.computeIfAbsent(k, loadFunction)
  }

  // maybe optimize later with cache batching
  def asyncMany(ks: List[K]): Fu[List[V]] = ks.map(async).sequenceFu

  def invalidate(k: K): Unit = cache invalidate k

  def preloadOne(k: K): Funit =
    if (cache.getIfPresent(k) == null) {
      // println(s"*** preload $name $k")
      incPreload()
      chm.computeIfAbsent(k, loadFunction).void
    } else funit

  // maybe optimize later with cach batching
  def preloadMany(ks: Seq[K]): Funit = ks.distinct.map(preloadOne).sequenceFu.void
  // def preloadSet(ks: Set[K]): Funit = ks.map(preloadOne).sequenceFu.void

  def setOneIfAbsent(k: K, v: => V): Unit =
    if (cache.getIfPresent(k) == null) {
      incPreload()
      cache.put(k, v)
    }

  private val loadFunction = new java.util.function.Function[K, Fu[V]] {
    def apply(k: K) = compute(k).withTimeout(
      duration = resultTimeout,
      error = lila.base.LilaException(s"Syncache $name $k timed out after $resultTimeout")
    )
      .mon(_ => recComputeNanos) // monitoring: record async time
      .addEffects(
        err => {
          logger.branch(name).warn(s"$err key=$k")
          chm remove k
        },
        res => {
          cache.put(k, res)
          chm remove k
        }
      )
  }

  private def waitForResult(k: K, fu: Fu[V], duration: FiniteDuration): V = {
    incWait()
    try {
      // monitoring: increment lock time
      lila.mon.measureIncMicros(_ => incWaitMicros)(fu await duration)
    } catch {
      case e: java.util.concurrent.TimeoutException =>
        incTimeout()
        default(k)
    }
  }

  private val incMiss = lila.mon.syncache.miss(name)
  private val incWait = lila.mon.syncache.wait(name)
  private val incPreload = lila.mon.syncache.preload(name)
  private val incTimeout = lila.mon.syncache.timeout(name)
  private val incWaitMicros = lila.mon.syncache.waitMicros(name)
  private val recComputeNanos = lila.mon.syncache.computeNanos(name)
}

object Syncache {

  sealed trait Strategy
  case object NeverWait extends Strategy
  case class AlwaysWait(duration: FiniteDuration) extends Strategy
  case class WaitAfterUptime(duration: FiniteDuration, uptimeSeconds: Int = 20) extends Strategy

  sealed trait ExpireAfter
  case class ExpireAfterAccess(duration: FiniteDuration) extends ExpireAfter
  case class ExpireAfterWrite(duration: FiniteDuration) extends ExpireAfter
}
