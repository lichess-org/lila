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

  def asLast = Msg.Last(
    text = text take 60,
    user = user,
    date = date,
    read = false
  )
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
      orig: User.ID,
      dest: User.ID,
      text: String
  ): Msg = Msg(
    _id = Id(ornicar.scalalib.Random nextString 8),
    thread = MsgThread.id(orig, dest),
    text = text,
    user = orig,
    date = DateTime.now
  )
}
