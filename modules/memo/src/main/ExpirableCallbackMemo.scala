package lila.memo

import akka.actor.{ Cancellable, Scheduler }
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

// calls a function when a key expires
final class ExpireCallbackMemo(
    scheduler: Scheduler,
    ttl: FiniteDuration,
    callback: String => Unit,
    initialCapacity: Int = 4096
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val timeouts = new ConcurrentHashMap[String, Cancellable](initialCapacity)

  def get(key: String): Boolean = timeouts contains key

  def put(key: String): Unit = timeouts
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
  def remove(key: String) = Option(timeouts remove key).foreach(_.cancel())

  def count = timeouts.size

  def keySet: Set[String] = timeouts.keySet.asScala.toSet
}
