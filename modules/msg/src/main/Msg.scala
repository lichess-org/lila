package lila.msg

case class Msg(text: String, user: UserId, date: Instant):

  def asLast =
    Msg.Last(
      text = text.take(60),
      user = user,
      date = date,
      read = false
    )

object Msg:

  case class Last(
      text: String,
      user: UserId,
      date: Instant,
      read: Boolean
  ):
    def unreadBy(userId: UserId) = !read && user != userId

  def make(text: String, user: UserId, date: Instant): Option[Msg] =
    val cleanText = lila.common.String.softCleanUp(text.take(8_000))
    cleanText.nonEmpty.option(
      Msg(
        text = cleanText,
        user = user,
        date = date
      )
    )
