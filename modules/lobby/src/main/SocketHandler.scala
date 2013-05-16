package lila.lobby

import actorApi._
import lila.common.PimpedJson._
import lila.socket.Handler
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.hub.actorApi.lobby._
import lila.user.{ User, Context }
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._
import play.api.libs.iteratee._

private[lobby] final class SocketHandler(
    socket: ActorRef,
    messenger: Messenger) {

  private def controller(
    socket: ActorRef,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
    case ("talk", o) ⇒ for {
      userId ← member.userId
      text ← o str "d"
      message ← messenger(userId, text)
    } messenger(userId, text) logFailure "[lobby] message" pipeTo socket
    case ("liveGames", o) ⇒ o str "d" foreach { ids ⇒
      socket ! LiveGames(uid, ids.split(' ').toList)
    }
  }

  def apply(
    uid: String,
    hook: Option[String],
    user: Option[User]): Fu[JsSocketHandler] = {
    val join = Join(uid = uid, user = user, hookOwnerId = hook)
    Handler(socket, uid, join) {
      case Connected(enum, member) ⇒
        controller(socket, uid, member) -> enum
    }
  }
}
