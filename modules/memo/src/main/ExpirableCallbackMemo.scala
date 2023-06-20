package lila.memo

import akka.actor.{ Cancellable, Scheduler }
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

// calls a function when a key expires
final class ExpireCallbackMemo[K](
    scheduler: Scheduler,
    ttl: FiniteDuration,
    callback: K => Unit,
    initialCapacity: Int = 4096
)(using Executor):

  private val timeouts = ConcurrentHashMap[K, Cancellable](initialCapacity)

  def get(key: K): Boolean = timeouts contains key

  def put(key: K): Unit = timeouts
    .compute(
      key,
      (_, canc) => {
        Option(canc).foreach(_.cancel())
        scheduler.scheduleOnce(ttl) {
          remove(key)
          callback(key)
        }
      }
    )
    .unit

  // does not call the expiration callback
  def remove(key: K): Unit = Option(timeouts remove key).foreach(_.cancel())

  def count: Int = timeouts.size

  def keySet: Set[K] = timeouts.keySet.asScala.toSet
