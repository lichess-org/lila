package lila.memo

import com.github.benmanes.caffeine.cache.*

import java.util.concurrent.TimeUnit
import scala.util.Success

import lila.common.Uptime

/** A synchronous cache from asynchronous computations. It will attempt to serve cached responses
  * synchronously. If none is available, it starts an async computation, and either waits for the result or
  * serves a default value.
  */
final class Syncache[K, V](
    name: String,
    initialCapacity: Int,
    compute: K => Fu[V],
    default: K => V,
    strategy: Syncache.Strategy,
    expireAfter: Syncache.ExpireAfter
)(using Executor):

  import Syncache.*

  // sync cached values
  private[memo] val cache: LoadingCache[K, Fu[V]] =
    Caffeine
      .newBuilder()
      .asInstanceOf[Caffeine[K, Fu[V]]]
      .initialCapacity(initialCapacity)
      .pipe: c =>
        expireAfter match
          case ExpireAfter.Access(duration) => c.expireAfterAccess(duration.toMillis, TimeUnit.MILLISECONDS)
          case ExpireAfter.Write(duration) => c.expireAfterWrite(duration.toMillis, TimeUnit.MILLISECONDS)
      .recordStats
      .build[K, Fu[V]](
        new CacheLoader[K, Fu[V]]:
          def load(k: K) =
            compute(k)
              .mon(_ => recCompute) // monitoring: record async time
              .recover { case e: Exception =>
                logger.branch(s"syncache $name").warn(s"key=$k", e)
                cache.invalidate(k)
                default(k)
              }
      )

  // get the value asynchronously, never blocks (preferred)
  def async(k: K): Fu[V] = cache.get(k)

  // get the value synchronously, might block depending on strategy
  def sync(k: K): V =
    val future = cache.get(k)
    future.value match
      case Some(Success(v)) => v
      case Some(_) =>
        cache.invalidate(k)
        default(k)
      case _ =>
        incMiss()
        strategy match
          case Strategy.NeverWait => default(k)
          case Strategy.WaitAfterUptime(duration, uptime) =>
            if Uptime.startedSinceSeconds(uptime) then waitForResult(k, future, duration)
            else default(k)

  // maybe optimize later with cache batching
  def asyncMany(ks: List[K]): Fu[List[V]] = ks.parallel(async)

  def invalidate(k: K): Unit = cache.invalidate(k)

  def preloadOne(k: K): Funit = async(k).void

  // maybe optimize later with cache batching
  def preloadMany(ks: Seq[K]): Funit = ks.distinct.parallelVoid(preloadOne)
  def preloadSet(ks: Set[K]): Funit = ks.toSeq.parallelVoid(preloadOne)

  def set(k: K, v: V): Unit = cache.put(k, fuccess(v))

  private def waitForResult(k: K, fu: Fu[V], duration: FiniteDuration): V =
    try
      lila.common.Chronometer.syncMon(_ => recWait):
        fu.await(duration, s"syncache:$name")
    catch
      case _: java.util.concurrent.TimeoutException =>
        incTimeout()
        default(k)

  private val incMiss = (() => lila.mon.syncache.miss(name).increment())
  private val incTimeout = (() => lila.mon.syncache.timeout(name).increment())
  private val recWait = lila.mon.syncache.wait(name)
  private val recCompute = lila.mon.syncache.compute(name)

object Syncache:

  enum Strategy:
    case NeverWait
    case WaitAfterUptime(duration: FiniteDuration, uptimeSeconds: Int = 20)

  enum ExpireAfter:
    case Access(duration: FiniteDuration)
    case Write(duration: FiniteDuration)
