package lila.memo

import akka.actor.{ Cancellable, Scheduler }

// calls a function when a key expires
final class ExpireCallbackMemo[K](
    scheduler: Scheduler,
    ttl: FiniteDuration,
    callback: K => Unit,
    initialCapacity: Int = 4096
)(using Executor):

  private val timeouts = scalalib.ConcurrentMap[K, Cancellable](initialCapacity)

  export timeouts.{ contains as get, size as count, keySet }

  def put(key: K): Unit = timeouts.compute(key): canc =>
    canc.foreach(_.cancel())
    scheduler
      .scheduleOnce(ttl):
        remove(key)
        callback(key)
      .some

  // does not call the expiration callback
  def remove(key: K): Unit = timeouts.remove(key).foreach(_.cancel())
