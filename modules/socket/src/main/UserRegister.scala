package lila.socket

import akka.actor._
import play.api.libs.json.JsObject

import actorApi.{ SocketLeave, SocketEnter }
import lila.hub.actorApi.{ SendTo, SendTos, WithUserIds }

private final class UserRegister extends Actor {

  override def preStart() {
    context.system.lilaBus.subscribe(self, 'users, 'socketDoor)
  }

  override def postStop() {
    super.postStop()
    context.system.lilaBus.unsubscribe(self)
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
  }

  private def sendTo(userId: String, msg: JsObject) {
    users get userId foreach { members =>
      members.foreachValue(_ push msg)
    }
  }
}
