package lila.socket

import play.api.Configuration
import io.lettuce.core._

final class Env(
    appConfig: Configuration,
    lifecycle: play.api.inject.ApplicationLifecycle,
    hub: lila.hub.Env
) {

  private val RedisUri = appConfig.get[String]("socket.redis.uri")

  val remoteSocket = new RemoteSocket(
    redisClient = RedisClient create RedisURI.create(RedisUri),
    notificationActor = hub.notification,
    lifecycle = lifecycle
  )
  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineUserIds: () => Set[String] = () => remoteSocket.onlineUserIds.get

  val isOnline: String => Boolean = userId => onlineUserIds() contains userId
}
