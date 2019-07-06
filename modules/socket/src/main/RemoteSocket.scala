package lila.socket

import play.api.libs.json._
import redis.clients.jedis._
import scala.concurrent.Future

import lila.hub.actorApi.round.{ MoveEvent, FinishGameId, Mlat }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.{ Deploy, Announce }
import lila.common.WithResource

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
    // case MoveEvent(gameId, fen, move) if watchedGameIds(gameId) =>
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
      // println(redisPool.getNumActive, "redisPool.getNumActive")
      // println(redisPool.getNumIdle, "redisPool.getNumIdle")
      // println(redisPool.getNumWaiters, "redisPool.getNumWaiters")
      send(Out.Mlat, ms.toString)
    case actorApi.SendToFlag(flag, payload) =>
      send(Out.TellFlag, flag, Json stringify payload)
    case WithUserIds(f) =>
      f(connectedUserIds)
  }

  private def onReceive(path: String, args: String) = path match {
    case In.Connect =>
      val userId = args
      connectedUserIds += userId
      bus.publish(lila.hub.actorApi.relation.ReloadOnlineFriends(userId), 'reloadOnlineFriends)
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
      parseIntOption(args) foreach setNb
    case path =>
      logger.warn(s"Invalid path $path")
  }

  private def send(path: String, args: String*) = WithResource(redisPool.getResource) {
    _.publish(chanOut, s"$path ${args mkString " "}")
  }

  Future {
    redisPool.getResource.subscribe(new JedisPubSub() {
      override def onMessage(channel: String, message: String): Unit = {
        val parts = message.split(" ", 2)
        onReceive(parts(0), ~parts.lift(1))
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
