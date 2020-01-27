package lila.msg

import org.joda.time.DateTime

import lila.user.User

case class Msg(
    text: String,
    user: User.ID,
    date: DateTime
) {

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
      text: String,
      user: User.ID
  ): Msg = Msg(
    text = text,
    user = user,
    date = DateTime.now
  )
}
