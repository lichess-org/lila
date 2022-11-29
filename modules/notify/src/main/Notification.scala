package lila.notify

import lila.common.paginator.Paginator
import lila.notify.MentionedInThread.PostId
import org.joda.time.DateTime
import reactivemongo.api.bson.Macros.Annotations.Key

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
  object Notifies extends OpaqueUserId[Notifies]

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

case class PlanStart(userId: UserId)  extends NotificationContent("planStart")  // BC
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
