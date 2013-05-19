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
    lobby: ActorRef,
    socket: ActorRef,
    messenger: Messenger) {

  private def controller(
    socket: ActorRef,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
    case ("join", o) ⇒ o str "d" foreach { id ⇒
      lobby ! BiteHook(id, uid, member.userId)
    }
    case ("cancel", o) ⇒ lobby ! CancelHook(uid)
    case ("talk", o) ⇒ for {
      userId ← member.userId
      text ← o str "d"
    } messenger(userId, text) logFailure "[lobby] message" foreach {
      _ foreach { socket ! _ }
    }
    case ("liveGames", o) ⇒ o str "d" foreach { ids ⇒
      socket ! LiveGames(uid, ids.split(' ').toList)
    }
  }

  def apply(uid: String, user: Option[User]): Fu[JsSocketHandler] = {
    val join = Join(uid = uid, user = user)
    Handler(socket, uid, join) {
      case Connected(enum, member) ⇒
        controller(socket, uid, member) -> enum
    }
  }
}
