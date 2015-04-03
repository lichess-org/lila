package lila.simul

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.security.Flood
import akka.actor.ActorSelection
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[simul] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    chat: ActorSelection,
    flood: Flood,
    exists: Simul.ID => Fu[Boolean]) {

  def join(
    simId: String,
    version: Int,
    uid: String,
    user: Option[User]): Fu[Option[JsSocketHandler]] =
    exists(simId) flatMap {
      _ ?? {
        for {
          socket ← socketHub ? Get(simId) mapTo manifest[ActorRef]
          join = Join(uid = uid, user = user, version = version)
          handler ← Handler(hub, socket, uid, join, user map (_.id)) {
            case Connected(enum, member) =>
              controller(socket, simId, uid, member) -> enum
          }
        } yield handler.some
      }
    }

  private def controller(
    socket: ActorRef,
    simId: String,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
    case ("startWatching", o) => o str "d" foreach { ids =>
      hub.actor.moveBroadcast ! StartWatching(uid, member, ids.split(' ').toSet)
    }
    case ("talk", o) => o str "d" foreach { text =>
      member.userId foreach { userId =>
        chat ! lila.chat.actorApi.UserTalk(simId, userId, text, socket)
      }
    }
  }
}
