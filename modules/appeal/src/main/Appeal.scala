package lila.appeal

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.id.AppealId
import lila.core.user.UserMark
import lila.ui.Icon

case class Appeal(
    @Key("_id") id: AppealId,
    msgs: Vector[AppealMsg], // chronological order, oldest first
    status: Appeal.Status, // from the moderators POV
    createdAt: Instant,
    updatedAt: Instant,
    // date of first player message without a mod reply
    // https://github.com/lichess-org/lila/issues/7564
    firstUnrepliedAt: Instant
):
  def userId: UserId = id.into(UserId)
  def isRead = status == Appeal.Status.Read
  def isMuted = status == Appeal.Status.Muted
  def isUnread = status == Appeal.Status.Unread

  def isAbout(userId: UserId) = id.is(userId)

  def post(text: String, by: UserId) =
    val msg = AppealMsg(by, text, nowInstant)
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
      case (_, msg) if isByMod(msg) => Vector.empty
      case (acc, msg) if msg.at.isAfter(nowInstant.minusWeeks(1)) => acc :+ msg
      case (acc, _) => acc

    val recentSize = recentWithoutMod.foldLeft(0)(_ + _.text.size)
    recentSize < Appeal.maxLength

  def unread = copy(status = Appeal.Status.Unread)
  def read = copy(status = Appeal.Status.Read)
  def toggleMute = if isMuted then read else copy(status = Appeal.Status.Muted)

  lazy val mutedSince: Option[Instant] = isMuted.so:
    msgs.reverse.takeWhile(m => !isByMod(m)).lastOption.map(_.at)

  def isByMod(msg: AppealMsg) = msg.by != id

object Appeal:

  given UserIdOf[Appeal] = _.id.userId

  enum Status:
    val key = Status.this.toString.toLowerCase
    case Unread, Read, Muted
  object Status:
    def apply(key: String) = values.find(_.key == key)

  case class WithUser(appeal: Appeal, user: User)

  val maxLength = 1100
  val maxLengthClient = 1000

  import play.api.data.*
  import play.api.data.Forms.*

  val form = Form:
    single("text" -> lila.common.Form.cleanNonEmptyText(minLength = 2, maxLength = maxLength))

  val modForm = Form:
    tuple(
      "text" -> lila.common.Form.cleanNonEmptyText,
      "process" -> boolean
    )

  private[appeal] case class SnoozeKey(snoozerId: UserId, appealId: AppealId)
  private[appeal] given UserIdOf[SnoozeKey] = _.snoozerId

  opaque type Filter = Option[UserMark]
  object Filter extends TotalWrapper[Filter, Option[UserMark]]:
    given Eq[Filter] = Eq.fromUniversalEquals
    extension (filter: Filter)
      def toggle(to: Filter) = (to != filter).option(to)
      def is(mark: UserMark) = filter.contains(mark)
      def key = filter.fold("clean")(_.key)

    val allWithIcon = List[(Filter, Either[Icon, String])](
      UserMark.troll.some -> Left(Icon.BubbleSpeech),
      UserMark.boost.some -> Left(Icon.LineGraph),
      UserMark.engine.some -> Left(Icon.Cogs),
      UserMark.alt.some -> Right("A"),
      none -> Left(Icon.User)
    )
    val byName: Map[String, Filter] =
      UserMark.byKey.view.mapValues(userMark => Filter(userMark.some)).toMap + ("clean" -> Filter(none))

case class AppealMsg(
    by: UserId,
    text: String,
    at: Instant
)
