package lila.appeal

import lila.user.User
import org.joda.time.DateTime

case class Appeal(
    _id: User.ID,
    msgs: Vector[AppealMsg],
    status: Appeal.Status,
    createdAt: DateTime,
    updatedAt: DateTime
) {
  def id = _id
  def isOpen = status != Appeal.Status.Closed
  def isAbout(userId: User.ID) = _id == userId

  def post(text: String, by: User) =
    copy(
      msgs = msgs :+ AppealMsg(
        by = by.id,
        text = text,
        at = DateTime.now
      ),
      updatedAt = DateTime.now,
      status = if (by.id == _id && status == Appeal.Status.Read) Appeal.Status.Unread else status
    )
}

object Appeal {

  sealed trait Status {
    val key = toString.toLowerCase
  }
  object Status {
    case object Unread extends Status
    case object Read   extends Status
    case object Closed extends Status
    case object Muted  extends Status
    val all                = List[Status](Unread, Read, Closed, Muted)
    def apply(key: String) = all.find(_.key == key)
  }

}

case class AppealMsg(
    by: User.ID,
    text: String,
    at: DateTime
)
