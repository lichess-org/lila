package lila.socket

import redis.clients.jedis._
import scala.concurrent.Future

private final class RemoteSocket(
    makeRedis: () => Jedis,
    chanIn: String,
    chanOut: String,
    lifecycle: play.api.inject.ApplicationLifecycle,
    bus: lila.common.Bus
) {

  private val clientIn = makeRedis()
  private val clientOut = makeRedis()

  Future {
    clientIn.subscribe(new JedisPubSub() {
      override def onMessage(channel: String, message: String): Unit = {
        println(s"Received $message")
        send(s"Received $message")
      }
    }, chanIn)
  }

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis clients...")
    Future {
      clientIn.quit()
      clientOut.quit()
    }
  }

  def send(data: String) = clientOut.publish(chanOut, data)
}
