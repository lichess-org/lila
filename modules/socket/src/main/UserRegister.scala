package lila.socket

import play.api.libs.json.JsObject

import actorApi.{ SocketLeave, SocketEnter }
import lila.hub.Trouper
import lila.hub.actorApi.socket.{ SendTo, SendTos, WithUserIds }
import lila.hub.actorApi.security.CloseAccount

private final class UserRegister(system: akka.actor.ActorSystem) extends Trouper {

  system.lilaBus.subscribe(this, 'socketEnter, 'socketLeave, 'accountClose, 'socketUsers)

  private val users = new MemberGroup[SocketMember](_.userId)

  val process: Trouper.Receive = {

    case SocketEnter(uid, member) => users.add(uid, member)

    case SocketLeave(uid, member) => users.remove(uid, member)

    case SendTo(userId, msg) => sendTo(userId, msg)

    case SendTos(userIds, msg) => userIds foreach { sendTo(_, msg) }

    case WithUserIds(f) => f(users.keys)

    case CloseAccount(userId) => userDo(userId)(_.end)
  }

  private def sendTo(userId: String, msg: JsObject): Unit =
    userDo(userId)(_ push msg)

  private def userDo(userId: String)(f: SocketMember => Unit): Unit =
    users get userId foreach { _ foreachValue f }
}
