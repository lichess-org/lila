package lila.notify

import lila.common.paginator.Paginator
import lila.notify.MentionedInThread.PostId
import org.joda.time.DateTime

case class NewNotification(notification: Notification, unreadNotifications: Int)

case class Notification(
    _id: String,
    notifies: Notification.Notifies,
    content: NotificationContent,
    read: Notification.NotificationRead,
    createdAt: DateTime
) {
  def id = _id

  def unread = !read.value

  def isMsg =
    content match {
      case _: PrivateMessage => true
      case _                 => false
    }
}

object Notification {

  case class UnreadCount(value: Int) extends AnyVal
  case class AndUnread(pager: Paginator[Notification], unread: UnreadCount)
  case class Notifies(value: String)          extends AnyVal with StringValue
  case class NotificationRead(value: Boolean) extends AnyVal

  def make(notifies: Notification.Notifies, content: NotificationContent): Notification = {
    val idSize = 8
    val id     = lila.common.ThreadLocalRandom nextString idSize
    new Notification(id, notifies, content, NotificationRead(false), DateTime.now)
  }
}

sealed abstract class NotificationContent(val key: String)

case class MentionedInThread(
    mentionedBy: MentionedInThread.MentionedBy,
    topic: MentionedInThread.Topic,
    topidId: MentionedInThread.TopicId,
    category: MentionedInThread.Category,
    postId: PostId
) extends NotificationContent("mention")

object MentionedInThread {
  case class MentionedBy(value: String) extends AnyVal with StringValue
  case class Topic(value: String)       extends AnyVal with StringValue
  case class TopicId(value: String)     extends AnyVal with StringValue
  case class Category(value: String)    extends AnyVal with StringValue
  case class PostId(value: String)      extends AnyVal with StringValue
}

case class InvitedToStudy(
    invitedBy: InvitedToStudy.InvitedBy,
    studyName: InvitedToStudy.StudyName,
    studyId: InvitedToStudy.StudyId
) extends NotificationContent("invitedStudy")

object InvitedToStudy {
  case class InvitedBy(value: String) extends AnyVal with StringValue
  case class StudyName(value: String) extends AnyVal with StringValue
  case class StudyId(value: String)   extends AnyVal with StringValue
}

case class PrivateMessage(
    user: PrivateMessage.Sender,
    text: PrivateMessage.Text
) extends NotificationContent("privateMessage")

object PrivateMessage {
  case class Sender(value: String) extends AnyVal with StringValue
  case class Text(value: String)   extends AnyVal with StringValue
}

case class TeamJoined(
    id: TeamJoined.Id,
    name: TeamJoined.Name
) extends NotificationContent("teamJoined")

object TeamJoined {
  case class Id(value: String)   extends AnyVal with StringValue
  case class Name(value: String) extends AnyVal with StringValue
}

case class TitledTournamentInvitation(
    id: String,
    text: String
) extends NotificationContent("titledTourney")

case class GameEnd(
    gameId: GameEnd.GameId,
    opponentId: Option[GameEnd.OpponentId],
    win: Option[GameEnd.Win]
) extends NotificationContent("gameEnd")

object GameEnd {
  case class GameId(value: String)     extends AnyVal
  case class OpponentId(value: String) extends AnyVal
  case class Win(value: Boolean)       extends AnyVal
}

case object ReportedBanned extends NotificationContent("reportedBanned")

case class RatingRefund(perf: String, points: Int) extends NotificationContent("ratingRefund")

case object CoachReview extends NotificationContent("coachReview")

case class PlanStart(userId: String)  extends NotificationContent("planStart")  // BC
case class PlanExpire(userId: String) extends NotificationContent("planExpire") // BC

case class CorresAlarm(
    gameId: lila.game.Game.ID,
    opponent: String
) extends NotificationContent("corresAlarm")

case class IrwinDone(
    userId: lila.user.User.ID
) extends NotificationContent("irwinDone")

case class GenericLink(
    url: String,
    title: Option[String],
    text: Option[String],
    icon: String
) extends NotificationContent("genericLink")
