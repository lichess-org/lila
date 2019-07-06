package lila.socket

import play.api.libs.json._
import redis.clients.jedis._
import scala.concurrent.Future

import lila.common.WithResource
import lila.hub.actorApi.round.{ MoveEvent, FinishGameId, Mlat }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.{ Deploy, Announce }

private final class RemoteSocket(
    redisPool: JedisPool,
    chanIn: String,
    chanOut: String,
    lifecycle: play.api.inject.ApplicationLifecycle,
    notificationActor: akka.actor.ActorSelection,
    setNb: Int => Unit,
    bus: lila.common.Bus
) {

  private object In {
    val Connect = "connect"
    val Disconnect = "disconnect"
    val Watch = "watch"
    val Notified = "notified"
    val Connections = "connections"
  }
  private object Out {
    val Move = "move"
    val TellUser = "tell/user"
    val TellUsers = "tell/users"
    val TellAll = "tell/all"
    val TellFlag = "tell/flag"
    val Mlat = "mlat"
  }

  private val connectedUserIds = collection.mutable.Set.empty[String]
  private val watchedGameIds = collection.mutable.Set.empty[String]

  bus.subscribeFun('moveEvent, 'finishGameId, 'socketUsers, 'deploy, 'announce, 'mlat, 'sendToFlag) {
    case MoveEvent(gameId, fen, move) =>
      if (watchedGameIds(gameId)) send(Out.Move, gameId, move, fen)
    case FinishGameId(gameId) if watchedGameIds(gameId) =>
      watchedGameIds -= gameId
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect connectedUserIds
      if (connectedUsers.nonEmpty) send(Out.TellUsers, connectedUsers mkString ",", Json stringify payload)
    case SendTo(userId, payload) if connectedUserIds(userId) =>
      send(Out.TellUser, userId, Json stringify payload)
    case d: Deploy =>
      send(Out.TellAll, Json stringify Json.obj("t" -> d.key))
    case Announce(msg) =>
      send(Out.TellAll, Json stringify Json.obj("t" -> "announce", "d" -> Json.obj("msg" -> msg)))
    case Mlat(ms) =>
      send(Out.Mlat, ms.toString)
      tick()
    case actorApi.SendToFlag(flag, payload) =>
      send(Out.TellFlag, flag, Json stringify payload)
    case WithUserIds(f) =>
      f(connectedUserIds)
  }

  private def onReceive(path: String, args: String) = path match {
    case In.Connect =>
      val userId = args
      connectedUserIds += userId
    case In.Disconnect =>
      val userId = args
      connectedUserIds -= args
    case In.Watch =>
      val gameId = args
      watchedGameIds += gameId
    case In.Notified =>
      val userId = args
      notificationActor ! lila.hub.actorApi.notify.Notified(userId)
    case In.Connections =>
      parseIntOption(args) foreach { nb =>
        setNb(nb)
        mon.connections(nb)
      }
    case path =>
      logger.warn(s"Invalid path $path")
  }

  /* Can fail if redis is offline, but must not propagate the exception,
   * because subscribeFun is synchronous, and this runs on the same thread
   * as the code that triggered the event */
  private def send(path: String, args: String*): Unit = try {
    WithResource(redisPool.getResource) { redis =>
      redis.publish(chanOut, s"$path ${args mkString " "}")
      redisMon.out()
    }
  } catch {
    case e: Exception =>
      logger.warn(s"RemoteSocket.out $path", e)
      redisMon.outError()
  }

  private def tick(): Unit = {
    redisMon.pool.active(redisPool.getNumActive)
    redisMon.pool.idle(redisPool.getNumIdle)
    redisMon.pool.waiters(redisPool.getNumWaiters)
    mon.sets.users(connectedUserIds.size)
    mon.sets.games(watchedGameIds.size)
  }

  private val mon = lila.mon.socket.remote
  private val redisMon = mon.redis

  Future {
    redisPool.getResource.subscribe(new JedisPubSub() {
      override def onMessage(channel: String, message: String): Unit = {
        val parts = message.split(" ", 2)
        onReceive(parts(0), ~parts.lift(1))
        redisMon.in()
      }
    }, chanIn)
  }

  lifecycle.addStopHook { () =>
    logger.info("Stopping the Redis pool...")
    Future {
      redisPool.close()
    }
  }
}
