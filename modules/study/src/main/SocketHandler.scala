package lila.study

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[study] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef) {

  def join(
    studyId: Study.ID,
    uid: String,
    userId: Option[User.ID],
    owner: Boolean): Fu[Option[JsSocketHandler]] = for {
    socket ← socketHub ? Get(studyId) mapTo manifest[ActorRef]
    join = Socket.Join(uid = uid, userId = userId, owner = owner)
    handler ← Handler(hub, socket, uid, join, userId) {
      case Socket.Connected(enum, member) =>
        (controller(socket, studyId, uid, member), enum, member)
    }
  } yield handler.some

  private def controller(
    socket: ActorRef,
    studyId: Study.ID,
    uid: String,
    member: Socket.Member): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v =>
      socket ! PingVersion(uid, v)
    }
  }
}
