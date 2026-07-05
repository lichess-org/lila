package lila.appeal

import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.user.UserMark
import lila.core.perm.Granter
import lila.ui.Icon

case class Appeal(
    @Key("_id") id: Appeal.Id,
    user: UserId,
    topic: AppealTopic,
    msgs: Vector[AppealMsg], // chronological order, oldest first
    status: Appeal.Status, // from the moderators POV
    createdAt: Instant,
    updatedAt: Instant,
    // date of first player message without a mod reply
    // https://github.com/lichess-org/lila/issues/7564
    firstUnrepliedAt: Instant
):
  def isRead = status == Appeal.Status.read
  def isUnread = status == Appeal.Status.unread
  def isClosed = status == Appeal.Status.closed
  def isRecent = updatedAt.isAfter(nowInstant.minusWeeks(1))
  def isOld = updatedAt.isBefore(nowInstant.minusMonths(6))

  def toggleClosed = if isClosed then read else copy(status = Appeal.Status.closed)

  def post(text: String, by: UserId) =
    val msg = AppealMsg(by, text, nowInstant)
    copy(
      msgs = msgs :+ msg,
      updatedAt = nowInstant,
      status =
        if isByMod(msg) && isUnread then Appeal.Status.read
        else if !isByMod(msg) && isRead then Appeal.Status.unread
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

    val recentCount = recentWithoutMod.size
    val recentSize = recentWithoutMod.foldLeft(0)(_ + _.text.size)
    recentSize < Appeal.maxLength && recentCount < 3

  def unread = copy(status = Appeal.Status.unread)
  def read = copy(status = Appeal.Status.read)

  def isByMod(msg: AppealMsg) = msg.by != id

object Appeal:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  given UserIdOf[Appeal] = _.user

  enum Status:
    case unread, read, closed
  object Status:
    def apply(key: String) = values.find(_.toString == key)

  case class WithUser(appeal: Appeal, user: User)

  val maxLength = 1100
  val maxLengthClient = 1000
  def maxLengthForMe(using Option[Me]) = if Granter.opt(_.Appeals) then 10_000 else maxLengthClient

  import play.api.data.*
  import play.api.data.Forms.*

  val form = Form:
    single("text" -> lila.common.Form.cleanNonEmptyText(minLength = 2, maxLength = maxLength))

  val modForm = Form:
    tuple(
      "text" -> lila.common.Form.cleanNonEmptyText,
      "process" -> boolean
    )

  def make(topic: AppealTopic, text: String)(using me: Me) =
    val now = nowInstant
    Appeal(
      id = Id(scalalib.ThreadLocalRandom.nextString(8)),
      user = me.userId,
      topic = topic,
      msgs = Vector(AppealMsg(me, text, now)),
      status = Status.unread,
      createdAt = now,
      updatedAt = now,
      firstUnrepliedAt = now
    )

  private[appeal] case class SnoozeKey(snoozerId: UserId, appealId: Id)
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

case class AppealMsg(by: UserId, text: String, at: Instant)
