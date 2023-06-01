package lila.socket

import java.util.concurrent.atomic.AtomicInteger
import com.github.benmanes.caffeine.cache.RemovalCause

// send a request to lila-ws and await a response
object SocketRequest:

  private val counter = AtomicInteger(0)

  private val inFlight = lila.memo.CacheApi.scaffeineNoScheduler
    .expireAfterWrite(30.seconds)
    .removalListener: (id, _, cause) =>
      if cause != RemovalCause.EXPLICIT then logger.warn(s"SocketRequest $id removed: $cause")
    .build[Int, Promise[String]]()
  private val asMap = inFlight.asMap()

  def apply[R](sendReq: Int => Unit, readRes: String => R)(using Executor): Fu[R] =
    val id = counter.getAndIncrement()
    sendReq(id)
    val promise = Promise[String]()
    inFlight.put(id, promise)
    promise.future map readRes

  private[socket] def onResponse(reqId: Int, response: String): Unit =
    asMap.remove(reqId).foreach(_ success response)
