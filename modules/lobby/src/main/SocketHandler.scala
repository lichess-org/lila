package lila.lobby

import akka.actor._
import akka.pattern.ask
import play.api.libs.json._
import play.api.libs.iteratee._

import actorApi._
import lila.common.PimpedJson._
import lila.socket.Handler
import lila.socket.actorApi.{ Connected ⇒ _, _ }
import lila.user.{ User, Context }
import lila.security.Flood
import makeTimeout.short

private[lobby] final class SocketHandler(socket: ActorRef, flood: Flood) {

  private def controller(
    socket: ActorRef,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) ⇒ o int "v" foreach { v ⇒ socket ! PingVersion(uid, v) }
    case ("talk", o) ⇒ for {
      txt ← o str "d"
      if member.canChat
      userId ← member.userId
      if flood.allowMessage(uid, txt)
    } socket ! Talk(userId, txt)
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
