package lidraughts.challenge

import lidraughts.hub.actorApi.map._
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User
import makeTimeout.short

private[challenge] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketHub: lidraughts.hub.ActorMapNew,
    pingChallenge: Challenge.ID => Funit
) {

  def join(
    challengeId: Challenge.ID,
    uid: Uid,
    userId: Option[User.ID],
    owner: Boolean,
    version: Option[SocketVersion]
  ): Fu[Option[JsSocketHandler]] = {
    val socket = socketHub getOrMake challengeId
    val join = Socket.Join(uid, userId, owner, version)
    Handler(hub, socket, uid, join) {
      case Socket.Connected(enum, member) =>
        (controller(socket, challengeId, uid, member), enum, member)
    } map some
  }

  private def controller(
    socket: akka.actor.ActorRef,
    challengeId: Challenge.ID,
    uid: Uid,
    member: Socket.Member
  ): Handler.Controller = {
    case ("p", o) => socket ! Ping(uid, o)
    case ("ping", _) if member.owner => pingChallenge(challengeId)
  }
}
