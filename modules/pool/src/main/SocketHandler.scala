package lila.pool

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import actorApi._
import akka.actor.ActorSelection
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.security.Flood
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[pool] final class SocketHandler(
    setupRepo: PoolSetupRepo,
    hub: lila.hub.Env,
    poolHub: ActorRef,
    chat: ActorSelection,
    flood: Flood) {

  def join(
    poolId: String,
    version: Int,
    uid: String,
    user: Option[User]): Fu[JsSocketHandler] = (setupRepo exists poolId) ?? {
    for {
      socket ← poolHub ? Get(poolId) mapTo manifest[ActorRef]
      join = Join(uid = uid, user = user, version = version)
      handler ← Handler(hub, socket, uid, join, user map (_.id)) {
        case Connected(enum, member) =>
          controller(socket, poolId, uid, member) -> enum
      }
    } yield handler
  }

  private def controller(
    socket: ActorRef,
    poolId: String,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
    case ("liveGames", o) => o str "d" foreach { ids =>
      socket ! LiveGames(uid, ids.split(' ').toList)
    }
    case ("talk", o) => o str "d" foreach { text =>
      member.userId foreach { userId =>
        chat ! lila.chat.actorApi.UserTalk(poolId, userId, text, socket)
      }
    }
  }
}

