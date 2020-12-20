package lila.msg

import lila.user.User
import lila.common.LightUser

case class MsgThread(
    id: MsgThread.Id,
    user1: User.ID,
    user2: User.ID,
    lastMsg: Msg.Last,
    del: Option[List[User.ID]] = None
) {

  def users = List(user1, user2)

  def other(userId: User.ID): User.ID = if (user1 == userId) user2 else user1
  def other(user: User): User.ID      = other(user.id)
  def other(user: LightUser): User.ID = other(user.id)

  def delBy(userId: User.ID) = del.exists(_ contains userId)

  def isPriority =
    !lastMsg.read && {
      user1 == User.lichessId || user2 == User.lichessId
    }
}

object MsgThread {

  case class Id(value: String) extends AnyVal

  case class WithMsgs(thread: MsgThread, msgs: List[Msg])

  case class WithContact(thread: MsgThread, contact: LightUser)

  case class Unread(thread: MsgThread)

  val idSep = '/'

  def id(u1: User.ID, u2: User.ID): Id =
    Id {
      sortUsers(u1, u2) match {
        case (user1, user2) => s"$user1$idSep$user2"
      }
    }

  def make(u1: User.ID, u2: User.ID, msg: Msg): MsgThread =
    sortUsers(u1, u2) match {
      case (user1, user2) =>
        MsgThread(
          id = id(user1, user2),
          user1 = user1,
          user2 = user2,
          lastMsg = msg.asLast
        )
    }

  private def sortUsers(u1: User.ID, u2: User.ID): (User.ID, User.ID) =
    if (u1 < u2) (u1, u2) else (u2, u1)
}
