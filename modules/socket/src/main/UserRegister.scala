package lidraughts.socket

import akka.actor._
import play.api.libs.json.JsObject

import actorApi.{ SocketLeave, SocketEnter }
import lidraughts.hub.actorApi.{ SendTo, SendTos, WithUserIds }
import lidraughts.hub.actorApi.security.CloseAccount

private final class UserRegister extends Actor {

  override def preStart(): Unit = {
    context.system.lidraughtsBus.subscribe(self, 'users, 'socketDoor, 'accountClose)
  }

  override def postStop(): Unit = {
    super.postStop()
    context.system.lidraughtsBus.unsubscribe(self)
  }

  type UID = String
  type UserId = String

  val users = new MemberGroup[SocketMember](_.userId)

  def receive = {

    case SendTo(userId, msg) => sendTo(userId, msg)

    case SendTos(userIds, msg) => userIds foreach { sendTo(_, msg) }

    case WithUserIds(f) => f(users.keys)

    case SocketEnter(uid, member) => users.add(uid, member)

    case SocketLeave(uid, member) => users.remove(uid, member)

    case CloseAccount(userId) => userDo(userId)(_.end)
  }

  private def sendTo(userId: String, msg: JsObject): Unit =
    userDo(userId)(_ push msg)

  private def userDo(userId: String)(f: SocketMember => Unit): Unit =
    users get userId foreach { _ foreachValue f }
}
