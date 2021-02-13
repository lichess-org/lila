package lila.appeal

import org.joda.time.DateTime

import lila.user.User

case class Appeal(
    _id: User.ID,
    msgs: Vector[AppealMsg],
    status: Appeal.Status, // from the moderators POV
    createdAt: DateTime,
    updatedAt: DateTime,
    // date of first player message without a mod reply
    // https://github.com/ornicar/lila/issues/7564
    firstUnrepliedAt: DateTime
) {
  def id       = _id
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
        else status,
      firstUnrepliedAt =
        if (isByMod(msg) || msgs.lastOption.exists(isByMod)) DateTime.now
        else firstUnrepliedAt
    )
  }

  def unread     = copy(status = Appeal.Status.Unread)
  def read       = copy(status = Appeal.Status.Read)
  def toggleMute = if (isMuted) read else copy(status = Appeal.Status.Muted)

  def isByMod(msg: AppealMsg) = msg.by != id
}

object Appeal {

  sealed trait Status {
    val key = toString.toLowerCase
  }
  object Status {
    case object Unread extends Status
    case object Read   extends Status
    case object Muted  extends Status
    val all                = List[Status](Unread, Read, Muted)
    def apply(key: String) = all.find(_.key == key)
  }

}

case class AppealMsg(
    by: User.ID,
    text: String,
    at: DateTime
)
