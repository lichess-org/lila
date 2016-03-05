package lila.lobby

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.lobby._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[lobby] final class SocketHandler(
    hub: lila.hub.Env,
    lobby: ActorRef,
    socket: ActorRef,
    blocking: String => Fu[Set[String]]) {

  lazy val RateLimit = new lila.memo.RateLimit(5, 1 minute, "lobby action")

  private def controller(
    socket: ActorRef,
    uid: String,
    member: Member): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v => socket ! PingVersion(uid, v) }
    case ("join", o) =>
      o str "d" foreach { id =>
        RateLimit(member.ip, s"$uid join hook $id") {
          lobby ! BiteHook(id, uid, member.user)
        }
      }
    case ("cancel", o) => RateLimit(member.ip, s"$uid cancel hooks") {
      lobby ! CancelHook(uid)
    }
    case ("joinSeek", o) => for {
      id <- o str "d"
      user <- member.user
    } RateLimit(member.ip, s"$uid join seek $id") {
      lobby ! BiteSeek(id, user)
    }
    case ("cancelSeek", o) => for {
      id <- o str "d"
      user <- member.user
    } RateLimit(member.ip, s"$uid cancel seek $id") {
      lobby ! CancelSeek(id, user)
    }
  }

  def apply(uid: String, ip: String, user: Option[User]): Fu[JsSocketHandler] =
    (user ?? (u => blocking(u.id))) flatMap { blockedUserIds =>
      val join = Join(uid = uid, ip = ip, user = user, blocking = blockedUserIds)
      Handler(hub, socket, uid, join, user map (_.id)) {
        case Connected(enum, member) =>
          (controller(socket, uid, member), enum, member)
      }
    }
}
