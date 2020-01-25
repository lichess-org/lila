package lila.msg

import org.joda.time.DateTime

import lila.user.User

case class Msg(
    _id: Msg.Id,
    thread: MsgThread.Id,
    text: String,
    user: User.ID,
    date: DateTime
) {
  def id = _id
}

object Msg {

  case class Id(value: String) extends AnyVal

  case class Last(
      text: String,
      user: User.ID,
      date: DateTime,
      read: Boolean
  )

  def make(
      thread: MsgThread.Id,
      last: Last
  ): Msg = Msg(
    _id = Id(ornicar.scalalib.Random nextString 8),
    thread = thread,
    text = last.text,
    user = last.user,
    date = last.date
  )
}
