package lila.lobby

import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent.duration._

import actorApi._
import lila.common.PimpedJson._
import lila.hub.actorApi.lobby._
import lila.pool.{ PoolApi, PoolConfig }
import lila.rating.RatingRange
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[lobby] final class SocketHandler(
    hub: lila.hub.Env,
    lobby: ActorRef,
    socket: ActorRef,
    poolApi: PoolApi,
    blocking: String => Fu[Set[String]]) {

  private def controller(socket: ActorRef, member: Member): Handler.Controller = {
    case ("join", o) =>
      o str "d" foreach { id =>
        lobby ! BiteHook(id, member.uid, member.user)
      }
    case ("cancel", _) =>
      lobby ! CancelHook(member.uid)
    case ("joinSeek", o) => for {
      id <- o str "d"
      user <- member.user
    } lobby ! BiteSeek(id, user)
    case ("cancelSeek", o) => for {
      id <- o str "d"
      user <- member.user
    } lobby ! CancelSeek(id, user)
    case ("idle", o) => socket ! SetIdle(member.uid, ~(o boolean "d"))
    // entering a pool
    case ("poolIn", o) => for {
      user <- member.user
      d <- o obj "d"
      id <- d str "id"
      ratingRange = d str "range" flatMap RatingRange.apply
    } {
      lobby ! CancelHook(member.uid) // in case there's one...
      poolApi.join(
        PoolConfig.Id(id),
        PoolApi.Joiner(
          userId = user.id,
          socketId = lila.socket.Socket.Uid(member.uid),
          ratingMap = user.ratingMap,
          ratingRange = ratingRange,
          engine = user.engine))
    }
    // leaving a pool
    case ("poolOut", o) => for {
      id <- o str "d"
      user <- member.user
    } poolApi.leave(PoolConfig.Id(id), user.id)
    // entering the hooks view
    case ("hookIn", _)  => lobby ! HookSub(member, true)
    // leaving the hooks view
    case ("hookOut", _) => socket ! HookSub(member, false)
  }

  def apply(uid: String, user: Option[User]): Fu[JsSocketHandler] =
    (user ?? (u => blocking(u.id))) flatMap { blockedUserIds =>
      val join = Join(uid = uid, user = user, blocking = blockedUserIds)
      Handler(hub, socket, uid, join) {
        case Connected(enum, member) =>
          (controller(socket, member), enum, member)
      }
    }
}
