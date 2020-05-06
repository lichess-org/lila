package lila.msg

import org.joda.time.DateTime

import lila.user.User

case class Msg(
    text: String,
    user: User.ID,
    date: DateTime
) {

  def asLast =
    Msg.Last(
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
  ) {
    def unreadBy(userId: User.ID) = !read && user != userId
  }

  def make(text: String, user: User.ID): Option[Msg] = {
    val cleanText = text.trim
    cleanText.nonEmpty option Msg(
      text = cleanText take 10_000,
      user = user,
      date = DateTime.now
    )
  }
}
