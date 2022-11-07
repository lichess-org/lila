package lila.notify

import lila.common.paginator.Paginator
import lila.game.Game
import lila.pref.NotifyAllows
import lila.user.User
import org.joda.time.DateTime

sealed abstract class NotificationContent(val key: String)

case class MentionedInThread(
    mentionedBy: String,
    topic: String,
    topidId: String,
    category: String,
    postId: String
) extends NotificationContent("mention")

case class StreamStart(
    streamerId: User.ID,
    streamerName: String
) extends NotificationContent("streamStart")

case class PrivateMessage(user: User.ID, text: String) extends NotificationContent("privateMessage")

case class InvitedToStudy(
    invitedBy: User.ID,
    studyName: String,
    studyId: String
) extends NotificationContent("invitedStudy")

case class TeamJoined(
    id: String,
    name: String
) extends NotificationContent("teamJoined")

case class TitledTournamentInvitation(
    id: String,
    text: String
) extends NotificationContent("titledTourney")

case class GameEnd(
    gameId: Game.ID,
    opponentId: Option[String],
    win: Option[GameEnd.Win]
) extends NotificationContent("gameEnd")

object GameEnd {
  type Win = Boolean
}

case object ReportedBanned extends NotificationContent("reportedBanned")

case class RatingRefund(perf: String, points: Int) extends NotificationContent("ratingRefund")

case object CoachReview extends NotificationContent("coachReview")

case class PlanStart(userId: User.ID) extends NotificationContent("planStart") // BC

case class PlanExpire(userId: User.ID) extends NotificationContent("planExpire") // BC

case class CorresAlarm(
    gameId: Game.ID,
    opponent: String
) extends NotificationContent("corresAlarm")

case class IrwinDone(
    userId: User.ID
) extends NotificationContent("irwinDone")

case class KaladinDone(
    userId: User.ID
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
    _id: String,
    notifies: User.ID,
    content: NotificationContent,
    read: Boolean,
    createdAt: DateTime
) {
  def to = notifies
}

object Notification {
  val idSize = 8

  case class AndUnread(pager: Paginator[Notification], unread: Int)
  case class IncrementUnread()

  def make(notifies: User.ID, content: NotificationContent): Notification =
    new Notification(
      lila.common.ThreadLocalRandom nextString idSize,
      notifies = notifies,
      content = content,
      read = false,
      createdAt = DateTime.now
    )
}
