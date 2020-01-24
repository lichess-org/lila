package lila.msg

import org.joda.time.DateTime

import lila.user.User

case class MsgThread(
    id: MsgThread.Id, // random
    user1: User.ID,
    user2: User.ID,
    lastMsg: Msg.Last
) {

  def users = List(user1, user2)

  def other(user: User) = if (user1 == user.id) user2 else user1
}

object MsgThread {

  case class Id(value: String) extends AnyVal

  def make(
      msg: Msg.Last,
      user1: User.ID,
      user2: User.ID
  ): MsgThread = MsgThread(
    id = Id(ornicar.scalalib.Random nextString 8),
    user1 = user1,
    user2 = user2,
    lastMsg = msg
  )
}
