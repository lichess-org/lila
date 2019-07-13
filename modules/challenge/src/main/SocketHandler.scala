package lila.challenge

import akka.actor._
import akka.pattern.ask

import lila.hub.actorApi.map._
import lila.socket.actorApi.{ Connected => _, _ }
import lila.socket.Handler
import lila.socket.Socket.{ Sri, SocketVersion }
import lila.user.User
import lila.common.ApiVersion

private[challenge] final class SocketHandler(
    hub: lila.hub.Env,
    socketMap: SocketMap,
    pingChallenge: Challenge.ID => Funit
) {

  import ChallengeSocket._

  def join(
    challengeId: Challenge.ID,
    sri: Sri,
    userId: Option[User.ID],
    owner: Boolean,
    version: Option[SocketVersion],
    apiVersion: ApiVersion
  ): Fu[JsSocketHandler] = {
    val socket = socketMap getOrMake challengeId
    socket.ask[Connected](Join(sri, userId, owner, version, _)) map {
      case Connected(enum, member) => Handler.iteratee(
        hub,
        controller(socket, challengeId, sri, member),
        member,
        socket,
        sri,
        apiVersion
      ) -> enum
    }
  }

  private def controller(
    socket: ChallengeSocket,
    challengeId: Challenge.ID,
    sri: Sri,
    member: Member
  ): Handler.Controller = {
    case ("ping", _) if member.owner => pingChallenge(challengeId)
  }
}
