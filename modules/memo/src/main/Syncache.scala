package lila.memo

import com.github.benmanes.caffeine.cache._
import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.util.chaining._
import scala.util.Success

import lila.common.Uptime

/** A synchronous cache from asynchronous computations.
  * It will attempt to serve cached responses synchronously.
  * If none is available, it starts an async computation,
  * and either waits for the result or serves a default value.
  */
final private[memo] class Syncache[K, V](
    name: String,
    initialCapacity: Int,
    compute: K => Fu[V],
    default: K => V,
    strategy: Syncache.Strategy,
    expireAfter: Syncache.ExpireAfter
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Syncache._

  // sync cached values
  private[memo] val cache: LoadingCache[K, Fu[V]] =
    Caffeine
      .newBuilder()
      .asInstanceOf[Caffeine[K, Fu[V]]]
      .initialCapacity(initialCapacity)
      .pipe { c =>
        expireAfter match {
          case ExpireAfterAccess(duration) => c.expireAfterAccess(duration.toMillis, TimeUnit.MILLISECONDS)
          case ExpireAfterWrite(duration)  => c.expireAfterWrite(duration.toMillis, TimeUnit.MILLISECONDS)
        }
      }
      .recordStats
      .build[K, Fu[V]](new CacheLoader[K, Fu[V]] {
        def load(k: K) =
          compute(k)
            .mon(_ => recCompute) // monitoring: record async time
            .recover { case e: Exception =>
              logger.branch(s"syncache $name").warn(s"key=$k", e)
              cache invalidate k
              default(k)
            }
      })

  // get the value asynchronously, never blocks (preferred)
  def async(k: K): Fu[V] = cache get k

  // get the value synchronously, might block depending on strategy
  def sync(k: K): V = {
    val future = cache get k
    future.value match {
      case Some(Success(v)) => v
      case Some(_) =>
        cache invalidate k
        default(k)
      case _ =>
        incMiss()
        strategy match {
          case NeverWait => default(k)
          case WaitAfterUptime(duration, uptime) =>
            if (Uptime startedSinceSeconds uptime) waitForResult(k, future, duration)
            else default(k)
        }
    }
  }

  // maybe optimize later with cache batching
  def asyncMany(ks: List[K]): Fu[List[V]] = ks.map(async).sequenceFu

  def invalidate(k: K): Unit = cache invalidate k

  def preloadOne(k: K): Funit = async(k).void

  // maybe optimize later with cach batching
  def preloadMany(ks: Seq[K]): Funit = ks.distinct.map(preloadOne).sequenceFu.void
  def preloadSet(ks: Set[K]): Funit  = ks.map(preloadOne).sequenceFu.void

  def set(k: K, v: V): Unit = cache.put(k, fuccess(v))

  private def waitForResult(k: K, fu: Fu[V], duration: FiniteDuration): V =
    try {
      lila.common.Chronometer.syncMon(_ => recWait) {
        fu.await(duration, "syncache")
      }
    } catch {
      case _: java.util.concurrent.TimeoutException =>
        incTimeout()
        default(k)
    }

  private val incMiss    = lila.mon.syncache.miss(name).increment _
  private val incTimeout = lila.mon.syncache.timeout(name).increment _
  private val recWait    = lila.mon.syncache.wait(name)
  private val recCompute = lila.mon.syncache.compute(name)
}

object Syncache {

  sealed trait Strategy
  case object NeverWait                                                         extends Strategy
  case class WaitAfterUptime(duration: FiniteDuration, uptimeSeconds: Int = 20) extends Strategy

  sealed trait ExpireAfter
  case class ExpireAfterAccess(duration: FiniteDuration) extends ExpireAfter
  case class ExpireAfterWrite(duration: FiniteDuration)  extends ExpireAfter
}
