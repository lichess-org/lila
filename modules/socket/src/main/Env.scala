package lila.socket

import akka.actor.{ CoordinatedShutdown, Scheduler }
import com.softwaremill.macwire.*
import io.lettuce.core.*
import play.api.Configuration

@Module
final class Env(appConfig: Configuration, shutdown: CoordinatedShutdown)(using Executor, Scheduler):
  private val redisClient = RedisClient create RedisURI.create(appConfig.get[String]("socket.redis.uri"))

  val remoteSocket: RemoteSocket = wire[RemoteSocket]

  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineIds = OnlineIds(() => remoteSocket.onlineUserIds.get)

  val isOnline = IsOnline(userId => remoteSocket.onlineUserIds.get contains userId)
