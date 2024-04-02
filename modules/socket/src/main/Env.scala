package lila.socket

import akka.actor.{ CoordinatedShutdown, Scheduler }
import com.softwaremill.macwire.*
import io.lettuce.core.*
import play.api.Configuration

import lila.core.socket.{ SocketRequester as _, * }

@Module
final class Env(appConfig: Configuration, shutdown: CoordinatedShutdown)(using Executor, Scheduler):

  private val redisClient = RedisClient.create(RedisURI.create(appConfig.get[String]("socket.redis.uri")))

  val userLag = new UserLagCache
  export userLag.{ getLagRating, put as putLag }

  val requester = new SocketRequester

  val remoteSocket: RemoteSocket = wire[RemoteSocket]
  export remoteSocket.{ kit, parallelKit }

  remoteSocket.subscribe("site-in", RemoteSocket.Protocol.In.baseReader)(remoteSocket.baseHandler)

  val onlineIds = OnlineIds(() => remoteSocket.onlineUserIds.get)

  val isOnline = IsOnline(userId => remoteSocket.onlineUserIds.get contains userId)
