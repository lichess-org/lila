package lila.challenge

import akka.actor._
import akka.pattern.ask

import lila.common.PimpedJson._
import lila.hub.actorApi.map._
import lila.socket.actorApi._
import lila.socket.Handler
import lila.user.User
import makeTimeout.short

private[challenge] final class SocketHandler(
    hub: lila.hub.Env,
    socketHub: ActorRef,
    pingChallenge: Challenge.ID => Funit)(implicit system: ActorSystem) {

  def join(
    challengeId: Challenge.ID,
    uid: String,
    userId: Option[User.ID],
    owner: Boolean): Fu[Option[JsFlow]] =
    socketHub ? Get(challengeId) mapTo manifest[ActorRef] map { socket =>
      Handler.actorRef { out =>
        val member = Socket.Member(out, userId, owner)
        socket ! Socket.AddMember(uid, member)
        Handler.props(hub, socket, member, uid, userId) {
          case ("p", o) => o int "v" foreach { v =>
            socket ! PingVersion(uid, v)
          }
          case ("ping", _) if member.owner => pingChallenge(challengeId)
        }
      }.some
    }

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
