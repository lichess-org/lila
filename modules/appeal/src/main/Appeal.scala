package lila.appeal

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.user.{ User, UserMark }

case class Appeal(
    @Key("_id") id: Appeal.Id,
    msgs: Vector[AppealMsg],
    status: Appeal.Status, // from the moderators POV
    createdAt: Instant,
    updatedAt: Instant,
    // date of first player message without a mod reply
    // https://github.com/lichess-org/lila/issues/7564
    firstUnrepliedAt: Instant
):

  def userId   = id.userId
  def isRead   = status == Appeal.Status.Read
  def isMuted  = status == Appeal.Status.Muted
  def isUnread = status == Appeal.Status.Unread

  def isAbout(userId: UserId) = id is userId

  def post(text: String, by: User) =
    val msg = AppealMsg(
      by = by.id,
      text = text,
      at = nowInstant
    )
    copy(
      msgs = msgs :+ msg,
      updatedAt = nowInstant,
      status =
        if isByMod(msg) && isUnread then Appeal.Status.Read
        else if !isByMod(msg) && isRead then Appeal.Status.Unread
        else status,
      firstUnrepliedAt =
        if isByMod(msg) || msgs.lastOption.exists(isByMod) || isRead then nowInstant
        else firstUnrepliedAt
    )

  def canAddMsg: Boolean =
    val recentWithoutMod = msgs.foldLeft(Vector.empty[AppealMsg]):
      case (_, msg) if isByMod(msg)                              => Vector.empty
      case (acc, msg) if msg.at isAfter nowInstant.minusWeeks(1) => acc :+ msg
      case (acc, _)                                              => acc

    val recentSize = recentWithoutMod.foldLeft(0)(_ + _.text.size)
    recentSize < Appeal.maxLength

  def unread     = copy(status = Appeal.Status.Unread)
  def read       = copy(status = Appeal.Status.Read)
  def toggleMute = if isMuted then read else copy(status = Appeal.Status.Muted)

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

  val form = Form:
    single("text" -> lila.common.Form.cleanNonEmptyText(minLength = 2, maxLength = maxLength))

  val modForm = Form:
    tuple(
      "text"    -> lila.common.Form.cleanNonEmptyText,
      "process" -> boolean
    )

  private[appeal] case class SnoozeKey(snoozerId: UserId, appealId: Appeal.Id)
  private[appeal] given UserIdOf[SnoozeKey] = _.snoozerId

  enum Filter:
    case Mark(mark: UserMark)
    case Clean

  extension (filter: Filter)
    def toggle(newFilter: Filter) =
      newFilter != filter option newFilter

    def is(mark: UserMark) = filter match
      case Filter.Mark(m) => m == mark
      case _              => false

    def key = filter match
      case Filter.Mark(m) => m.key
      case Filter.Clean   => filter.toString.toLowerCase

    def boost  = filter.is(UserMark.Boost)
    def engine = filter.is(UserMark.Engine)
    def troll  = filter.is(UserMark.Troll)
    def alt    = filter.is(UserMark.Alt)
    def clean  = filter == Filter.Clean

  object Filter:
    val Boost  = Filter.Mark(UserMark.Boost)
    val Engine = Filter.Mark(UserMark.Engine)
    val Troll  = Filter.Mark(UserMark.Troll)
    val Alt    = Filter.Mark(UserMark.Alt)

    def apply(name: String): Option[Filter] =
      if name.toLowerCase() == "clean"
      then Filter.Clean.some
      else UserMark.indexed.get(name).map(Filter.Mark.apply)

case class AppealMsg(
    by: UserId,
    text: String,
    at: Instant
)
