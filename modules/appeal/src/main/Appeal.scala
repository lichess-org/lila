package lila.appeal

import lila.user.User
import org.joda.time.DateTime

case class Appeal(
    _id: User.ID,
    msgs: Vector[AppealMsg],
    status: Appeal.Status, // from the moderators POV
    createdAt: DateTime,
    updatedAt: DateTime
) {
  def id       = _id
  def isOpen   = status != Appeal.Status.Closed
  def isMuted  = status == Appeal.Status.Muted
  def isUnread = status == Appeal.Status.Unread

  def isAbout(userId: User.ID) = _id == userId

  def post(text: String, by: User) = {
    val msg = AppealMsg(
      by = by.id,
      text = text,
      at = DateTime.now
    )
    copy(
      msgs = msgs :+ msg,
      updatedAt = DateTime.now,
      status =
        if (isByMod(msg) && status == Appeal.Status.Unread) Appeal.Status.Read
        else if (!isByMod(msg) && status == Appeal.Status.Read) Appeal.Status.Unread
        else status
    )
  }

  def close = copy(status = Appeal.Status.Closed)
  def open  = copy(status = Appeal.Status.Read)
  def mute  = copy(status = Appeal.Status.Muted)

  def isByMod(msg: AppealMsg) = msg.by != id
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
