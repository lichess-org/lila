package lila.socket

import play.api.libs.json._
import redis.clients.jedis._
import scala.concurrent.Future

import lila.hub.actorApi.round.{ MoveEvent, FinishGameId }
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }

private final class RemoteSocket(
    makeRedis: () => Jedis,
    chanIn: String,
    chanOut: String,
    lifecycle: play.api.inject.ApplicationLifecycle,
    notificationActor: akka.actor.ActorSelection,
    bus: lila.common.Bus
) {

  private val clientIn = makeRedis()
  private val clientOut = makeRedis()

  Future {
    clientIn.subscribe(new JedisPubSub() {
      override def onMessage(channel: String, message: String): Unit = {
        try {
          Json.parse(message) match {
            case o: JsObject => o str "path" foreach { onReceive(_, o) }
            case _ => logger warn s"Invalid message $message"
          }
        } catch {
          case _: Exception => logger.warn(s"Can't parse remote socket message $message")
        }
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

  private val connectedUserIds = collection.mutable.Set.empty[String]
  private val watchedGameIds = collection.mutable.Set.empty[String]

  bus.subscribeFun('moveEvent, 'finishGameId, 'socketUsers) {
    case MoveEvent(gameId, fen, move) if watchedGameIds(gameId) => send(Json.obj(
      "path" -> "/move",
      "gameId" -> gameId,
      "fen" -> fen,
      "move" -> move
    ))
    case FinishGameId(gameId) if watchedGameIds(gameId) => watchedGameIds -= gameId
    case SendTos(userIds, payload) =>
      val connectedUsers = userIds intersect connectedUserIds
      if (connectedUsers.nonEmpty) send(Json.obj(
        "path" -> "/tell/users",
        "users" -> connectedUsers,
        "payload" -> payload
      ))
    case SendTo(userId, payload) if connectedUserIds(userId) =>
      send(Json.obj(
        "path" -> "/tell/user",
        "user" -> userId,
        "payload" -> payload
      ))
    case WithUserIds(f) => f(connectedUserIds)
  }

  private def onReceive(path: String, data: JsObject) = path match {
    case "/connect" => data str "user" foreach { userId =>
      connectedUserIds += userId
      bus.publish(lila.hub.actorApi.relation.ReloadOnlineFriends(userId), 'reloadOnlineFriends)
    }
    case "/disconnect" => data str "user" foreach { userId =>
      connectedUserIds -= userId
    }
    case "/watch" => data str "game" foreach { gameId =>
      watchedGameIds += gameId
    }
    case "/notified" => data str "user" foreach { userId =>
      notificationActor ! lila.hub.actorApi.notify.Notified(userId)
    }
    case path => logger.warn(s"Invalid path $path")
  }

  private def send(data: JsObject) = clientOut.publish(chanOut, Json stringify data)
}
