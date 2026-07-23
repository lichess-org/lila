package lila.appeal

import reactivemongo.api.bson.Macros.Annotations.Key

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
    firstUnrepliedAt: Instant,
    muted: Boolean = false, // new appeal posts of the user are ignored
    closedUntil: Option[Instant] = None // user must wait a certain duration
):
  def isRead = status == Appeal.Status.read
  def isUnread = status == Appeal.Status.unread
  def isClosed = status == Appeal.Status.closed
  def isOpen = !isClosed
  def isRecent = updatedAt.isAfter(nowInstant.minusWeeks(1))
  def isOld = updatedAt.isBefore(nowInstant.minusMonths(6))

  def toggleClosed(v: Boolean) =
    if v then copy(status = Appeal.Status.closed)
    else copy(status = Appeal.Status.read).sleep(none)

  def toggleRead(v: Boolean) =
    copy(status = if v then Appeal.Status.read else Appeal.Status.unread)

  def sleep(months: Option[Int]) = copy(closedUntil = months.map(nowInstant.plusMonths))

  def post(text: String, by: UserId, muted: Boolean) =
    val msg = AppealMsg(by, text, nowInstant)
    copy(
      msgs = msgs :+ msg,
      updatedAt = nowInstant,
      status =
        if isByMod(msg) && isUnread then Appeal.Status.read
        else if !isByMod(msg) && isRead && !muted then Appeal.Status.unread
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

  def isByMod(msg: AppealMsg) = msg.by != user

  def modIds = msgs.collect { case msg if isByMod(msg) => msg.by }.distinct.toList

  def participated(modId: UserId) = msgs.exists(_.by.is(modId))

  def isLast(msg: AppealMsg) = msgs.lastOption.contains(msg)

  def modShowUrl = s"${routes.Appeal.modShow(user, topic)}#appeal-last-msg"

opaque type UserAppeals = Map[AppealTopic, Appeal]
object UserAppeals extends TotalWrapper[UserAppeals, Map[AppealTopic, Appeal]]:
  extension (appeals: UserAppeals)
    def muted = appeals.values.exists(_.muted)
    def get = appeals.get

object Appeal:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  given UserIdOf[Appeal] = _.user

  enum Status:
    case unread, read, closed
    def key = toString
  object Status:
    def apply(key: String) = values.find(_.key == key)

  val maxLength = 1100

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

case class AppealMsg(by: UserId, text: String, at: Instant)
