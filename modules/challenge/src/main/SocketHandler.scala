package lidraughts.challenge

import akka.actor._
import akka.pattern.ask

import lidraughts.hub.actorApi.map._
import lidraughts.socket.actorApi.{ Connected => _, _ }
import lidraughts.socket.Handler
import lidraughts.socket.Socket.{ Uid, SocketVersion }
import lidraughts.user.User
import makeTimeout.short

private[challenge] final class SocketHandler(
    hub: lidraughts.hub.Env,
    socketMap: SocketMap,
    pingChallenge: Challenge.ID => Funit
) {

  import ChallengeSocket._

  def join(
    challengeId: Challenge.ID,
    uid: Uid,
    userId: Option[User.ID],
    owner: Boolean,
    version: Option[SocketVersion]
  ): Fu[JsSocketHandler] = {
    val socket = socketMap getOrMake challengeId
    socket.ask[Connected](Join(uid, userId, owner, version, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        controller(socket, challengeId, uid, member),
        member,
        socket,
        uid
      ) -> enum
    }
  }

  private def controller(
    socket: ChallengeSocket,
    challengeId: Challenge.ID,
    uid: Uid,
    member: Member
  ): Handler.Controller = {
    case ("p", o) => socket ! Ping(uid, o)
    case ("ping", _) if member.owner => pingChallenge(challengeId)
  }
}
