package lila.socket

import scredis._

private final class RemoteSocket(
    redis: Redis,
    chanIn: String,
    chanOut: String,
    bus: lila.common.Bus
) {

  redis.subscriber.subscribe(chanIn) {
    case message @ PubSubMessage.Message(channel, messageBytes) =>
      val msg = message.readAs[String]()
      send(s"Received $msg")
    case PubSubMessage.Subscribe(channel, subscribedChannelsCount) => logger.info(
      s"Subscribed to redis channel $channel"
    )
  }

  def send(data: String) = redis.publish(chanOut, data)
}
