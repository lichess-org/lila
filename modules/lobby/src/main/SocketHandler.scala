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
    user: Option[User],
    member: Member): Handler.Controller = {
    case ("talk", o) ⇒ for {
      txt ← o str "d"
      if flood.allowMessage(uid, txt)
      username ← user map (_.username)
    } socket ! Talk(username, txt)
    case ("p", o) ⇒ o int "v" foreach { v ⇒
      socket ! PingVersion(uid, v)
    }
    case ("liveGames", o) ⇒ o str "d" foreach { ids ⇒
      socket ! LiveGames(uid, ids.split(' ').toList)
    }
  }

  def join(
    uid: String,
    version: Int,
    hook: Option[String])(ctx: Context): Fu[JsSocketHandler] = {
    val join = Join(
      uid = uid,
      userId = ctx.userId,
      version = version,
      hookOwnerId = hook)
    Handler(socket, uid, join) {
      case Connected(enum, member) ⇒
        controller(socket, uid, ctx.me, member) -> enum
    }
  }
}
