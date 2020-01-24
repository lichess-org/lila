package lila.msg

import org.joda.time.DateTime

import lila.user.User

case class MsgThread(
    _id: MsgThread.Id,    // random
    users: List[User.ID], // unique
    lastMsg: Msg.Last
)

object MsgThread {

  case class Id(value: String) extends AnyVal

  def make(
      msg: Msg.Last,
      orig: User.ID,
      dest: User.ID
  ): MsgThread = MsgThread(
    _id = Id(ornicar.scalalib.Random nextString 8),
    users = List(orig, dest).sorted,
    lastMsg = msg
  )
}
