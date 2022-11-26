package lila.notify

import org.joda.time.DateTime
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.common.paginator.Paginator
import lila.notify.MentionedInThread.PostId
import lila.pref.NotifyAllows
import lila.user.User

sealed abstract class NotificationContent(val key: String)

case class MentionedInThread(
    mentionedBy: UserId,
    topic: String,
    topidId: TopicId,
    category: String,
    postId: PostId
) extends NotificationContent("mention")

case class StreamStart(
    streamerId: UserId,
    streamerName: String
) extends NotificationContent("streamStart")

case class PrivateMessage(user: UserId, text: String) extends NotificationContent("privateMessage")

case class InvitedToStudy(
    invitedBy: UserId,
    studyName: String,
    studyId: StudyId
) extends NotificationContent("invitedStudy")

case class TeamJoined(
    id: TeamId,
    name: String
) extends NotificationContent("teamJoined")

case class TitledTournamentInvitation(
    id: TourId,
    text: String
) extends NotificationContent("titledTourney")

case class GameEnd(
    gameId: GameFullId, // not sure it's safe to change this from GameId, could it
    opponentId: Option[String], // break pre-scala3 notifications in the db?
    win: Option[GameEnd.Win]
) extends NotificationContent("gameEnd")

object GameEnd:
  type Win = Boolean

case object ReportedBanned extends NotificationContent("reportedBanned")

case class RatingRefund(perf: String, points: Int) extends NotificationContent("ratingRefund")

case object CoachReview extends NotificationContent("coachReview")

case class PlanStart(userId: UserId) extends NotificationContent("planStart") // BC

case class PlanExpire(userId: UserId) extends NotificationContent("planExpire") // BC

case class CorresAlarm(
    gameId: GameId,
    opponent: String
) extends NotificationContent("corresAlarm")

case class IrwinDone(
    userId: UserId
) extends NotificationContent("irwinDone")

case class KaladinDone(
    userId: UserId
) extends NotificationContent("kaladinDone")

case class GenericLink(
    url: String,
    title: Option[String],
    text: Option[String],
    icon: String
) extends NotificationContent("genericLink")

case class PushNotification(
    to: Iterable[NotifyAllows],
    content: NotificationContent,
    params: Iterable[(String, String)] = Nil
)

private[notify] case class Notification(
    @Key("_id") id: Notification.Id,
    notifies: UserId,
    content: NotificationContent,
    read: Boolean,
    createdAt: DateTime
):
  def to = notifies

object Notification:
  val idSize = 8

  case class AndUnread(pager: Paginator[Notification], unread: Int)
  case class IncrementUnread()

  def make(to: UserId, content: NotificationContent): Notification =
    new Notification(
      lila.common.ThreadLocalRandom nextString idSize,
      notifies = to,
      content = content,
      read = false,
      createdAt = DateTime.now
    )
/*
case class NewNotification(notification: Notification, unreadNotifications: Notification.UnreadCount)

case class Notification(
    @Key("_id") id: Notification.Id,
    notifies: Notification.Notifies,
    content: NotificationContent,
    read: Notification.NotificationRead,
    createdAt: DateTime
):
  def unread = read.no

  def isMsg =
    content match
      case _: PrivateMessage => true
      case _                 => false

object Notification:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  opaque type UnreadCount = Int
  object UnreadCount extends OpaqueInt[UnreadCount]

  case class AndUnread(pager: Paginator[Notification], unread: UnreadCount)

  opaque type Notifies = String // the user being notified
  object Notifies extends OpaqueString[Notifies]

  opaque type NotificationRead = Boolean
  object NotificationRead extends YesNo[NotificationRead]

  def make(notifies: UserId, content: NotificationContent): Notification =
    val idSize = 8
    val id     = lila.common.ThreadLocalRandom nextString idSize
    Notification(Id(id), notifies into Notifies, content, NotificationRead(false), DateTime.now)

sealed abstract class NotificationContent(val key: String)

case class MentionedInThread(
    mentionedBy: UserId,
    topic: MentionedInThread.Topic,
    topidId: MentionedInThread.TopicId,
    category: MentionedInThread.Category,
    postId: PostId
) extends NotificationContent("mention")

object MentionedInThread:
  opaque type Topic = String
  object Topic extends OpaqueString[Topic]
  opaque type TopicId = String
  object TopicId extends OpaqueString[TopicId]
  opaque type Category = String
  object Category extends OpaqueString[Category]
  opaque type PostId = String
  object PostId extends OpaqueString[PostId]

case class InvitedToStudy(
    invitedBy: UserId,
    studyName: StudyName,
    studyId: StudyId
) extends NotificationContent("invitedStudy")

case class PrivateMessage(
    user: UserId,
    text: String
) extends NotificationContent("privateMessage")

case class TeamJoined(id: TeamId, name: lila.hub.LightTeam.TeamName) extends NotificationContent("teamJoined")

case class TitledTournamentInvitation(
    id: TourId,
    text: String
) extends NotificationContent("titledTourney")

case class GameEnd(
    fullId: GameFullId,
    opponentId: Option[UserId],
    win: Option[Win]
) extends NotificationContent("gameEnd")

case object ReportedBanned extends NotificationContent("reportedBanned")

case class RatingRefund(perf: String, points: Int) extends NotificationContent("ratingRefund")

case object CoachReview extends NotificationContent("coachReview")

case class PlanStart(userId: String)  extends NotificationContent("planStart")  // BC
case class PlanExpire(userId: String) extends NotificationContent("planExpire") // BC

case class CorresAlarm(
    gameId: GameId,
    opponent: String
) extends NotificationContent("corresAlarm")

case class IrwinDone(
    userId: lila.user.User.ID
) extends NotificationContent("irwinDone")

case class KaladinDone(
    userId: lila.user.User.ID
) extends NotificationContent("kaladinDone")

case class GenericLink(
    url: String,
    title: Option[String],
    text: Option[String],
    icon: String
) extends NotificationContent("genericLink")
*/
