package lila.challenge

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask

import akka.actor.ActorSelection
import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[challenge] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    pingChallenge: Challenge.ID => Funit) {

  def join(
    challengeId: Challenge.ID,
    uid: String,
    userId: Option[User.ID],
    owner: Boolean): Fu[Option[JsSocketHandler]] = for {
    socket ← socketHub ? Get(challengeId) mapTo manifest[ActorRef]
    join = Socket.Join(uid = uid, userId = userId, owner = owner)
    handler ← Handler(hub, socket, uid, join) {
      case Socket.Connected(enum, member) =>
        (controller(socket, challengeId, uid, member), enum, member)
    }
  } yield handler.some

  private def controller(
    socket: ActorRef,
    challengeId: Challenge.ID,
    uid: String,
    member: Socket.Member): Handler.Controller = {
    case ("p", o) => o int "v" foreach { v =>
      socket ! PingVersion(uid, v)
    }
    case ("ping", _) if member.owner => pingChallenge(challengeId)
  }
}
