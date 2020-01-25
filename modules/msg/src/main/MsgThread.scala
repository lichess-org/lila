package lila.msg

import org.joda.time.DateTime

import lila.user.User

case class MsgThread(
    id: MsgThread.Id,
    user1: User.ID,
    user2: User.ID,
    lastMsg: Option[Msg.Last]
) {

  def users = List(user1, user2)

  def other(user: User) = if (user1 == user.id) user2 else user1

  def setRead = copy(lastMsg = lastMsg.map(_.copy(read = true)))
}

object MsgThread {

  case class Id(value: String) extends AnyVal

  case class WithMsgs(thread: MsgThread, msgs: List[Msg])

  def id(u1: User.ID, u2: User.ID): Id = Id {
    sortUsers(u1, u2) match {
      case (user1, user2) => s"$user1/$user2"
    }
  }

  def make(u1: User.ID, u2: User.ID): MsgThread = sortUsers(u1, u2) match {
    case (user1, user2) =>
      s"$user1/$user2"
      MsgThread(
        id = id(user1, user2),
        user1 = user1,
        user2 = user2,
        lastMsg = none
      )
  }

  private def sortUsers(u1: User.ID, u2: User.ID): (User.ID, User.ID) =
    if (u1 < u2) (u1, u2) else (u2, u1)
}
