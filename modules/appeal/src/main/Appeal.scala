package lila.appeal

import org.joda.time.DateTime
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.user.User

case class Appeal(
    @Key("_id") id: Appeal.Id,
    msgs: Vector[AppealMsg],
    status: Appeal.Status, // from the moderators POV
    createdAt: DateTime,
    updatedAt: DateTime,
    // date of first player message without a mod reply
    // https://github.com/lichess-org/lila/issues/7564
    firstUnrepliedAt: DateTime
):

  inline def userId = id.userId
  def isRead        = status == Appeal.Status.Read
  def isMuted       = status == Appeal.Status.Muted
  def isUnread      = status == Appeal.Status.Unread

  def isAbout(userId: UserId) = id is userId

  def post(text: String, by: User) =
    val msg = AppealMsg(
      by = by.id,
      text = text,
      at = DateTime.now
    )
    copy(
      msgs = msgs :+ msg,
      updatedAt = DateTime.now,
      status =
        if (isByMod(msg) && isUnread) Appeal.Status.Read
        else if (!isByMod(msg) && isRead) Appeal.Status.Unread
        else status,
      firstUnrepliedAt =
        if (isByMod(msg) || msgs.lastOption.exists(isByMod) || isRead) DateTime.now
        else firstUnrepliedAt
    )

  def canAddMsg: Boolean =
    val recentWithoutMod = msgs.foldLeft(Vector.empty[AppealMsg]) {
      case (_, msg) if isByMod(msg)                                => Vector.empty
      case (acc, msg) if msg.at isAfter DateTime.now.minusWeeks(1) => acc :+ msg
      case (acc, _)                                                => acc
    }
    val recentSize = recentWithoutMod.foldLeft(0)(_ + _.text.size)
    recentSize < Appeal.maxLength

  def unread     = copy(status = Appeal.Status.Unread)
  def read       = copy(status = Appeal.Status.Read)
  def toggleMute = if (isMuted) read else copy(status = Appeal.Status.Muted)

  def isByMod(msg: AppealMsg) = msg.by != id

object Appeal:

  opaque type Id = String
  object Id extends OpaqueUserId[Id]

  given UserIdOf[Appeal] = _.id.userId

  enum Status:
    val key = Status.this.toString.toLowerCase
    case Unread, Read, Muted
  object Status:
    def apply(key: String) = values.find(_.key == key)

  case class WithUser(appeal: Appeal, user: User)

  val maxLength       = 1100
  val maxLengthClient = 1000

  import play.api.data.*
  import play.api.data.Forms.*

  val form =
    Form[String](
      single("text" -> lila.common.Form.cleanNonEmptyText(minLength = 2, maxLength = maxLength))
    )

  val modForm =
    Form[String](
      single("text" -> lila.common.Form.cleanNonEmptyText)
    )

  private[appeal] case class SnoozeKey(snoozerId: UserId, appealId: Appeal.Id)
  private[appeal] given UserIdOf[SnoozeKey] = _.snoozerId

case class AppealMsg(
    by: UserId,
    text: String,
    at: DateTime
)
