package lila.socket

import akka.actor.{ CoordinatedShutdown, Scheduler }
import com.softwaremill.macwire._
import io.lettuce.core._
import play.api.Configuration

@Module
final class Env(appConfig: Configuration, shutdown: CoordinatedShutdown)(implicit
    ec: scala.concurrent.ExecutionContext,
    scheduler: Scheduler
) {
  private val redisClient = RedisClient create RedisURI.create(appConfig.get[String]("socket.redis.uri"))

  val remoteSocket: RemoteSocket = wire[RemoteSocket]

  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineIds = new OnlineIds(() => remoteSocket.onlineUserIds.get)

  val isOnline = new IsOnline(userId => remoteSocket.onlineUserIds.get contains userId)
}
