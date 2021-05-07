package lila.socket

import akka.actor._
import com.softwaremill.macwire._
import io.lettuce.core._
import play.api.Configuration

@Module
final class Env(
    appConfig: Configuration,
    shutdown: CoordinatedShutdown,
    notification: lila.hub.actors.Notification
)(implicit
    ec: scala.concurrent.ExecutionContext,
    akka: ActorSystem
) {

  private val RedisUri = appConfig.get[String]("socket.redis.uri")

  private val redisClient = RedisClient create RedisURI.create(RedisUri)

  val remoteSocket: RemoteSocket = wire[RemoteSocket]

  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineIds = new OnlineIds(() => remoteSocket.onlineUserIds.get)

  val isOnline = new IsOnline(userId => remoteSocket.onlineUserIds.get contains userId)
}
