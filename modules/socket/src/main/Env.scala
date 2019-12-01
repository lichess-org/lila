package lila.socket

import com.softwaremill.macwire._
import io.lettuce.core._
import play.api.Configuration

final class Env(
    appConfig: Configuration,
    lifecycle: play.api.inject.ApplicationLifecycle,
    notification: lila.hub.actors.Notification
) {

  private val RedisUri = appConfig.get[String]("socket.redis.uri")

  private val redisClient = RedisClient create RedisURI.create(RedisUri)

  val remoteSocket = wire[RemoteSocket]

  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineUserIds: () => Set[String] = () => remoteSocket.onlineUserIds.get

  val isOnline: String => Boolean = userId => onlineUserIds() contains userId
}
