package lila.core
package notify

import alleycats.Zero

import lila.core.id.*
import lila.core.study.data.StudyName
import lila.core.userId.*

opaque type UnreadCount = Int
object UnreadCount extends RelaxedOpaqueInt[UnreadCount]:
  given Zero[UnreadCount] = Zero(0)

case class NotifiedBatch(userIds: Iterable[UserId])

enum NotificationContent(val key: String):
  case IrwinDone(userId: UserId) extends NotificationContent("irwinDone")
  case KaladinDone(userId: UserId) extends NotificationContent("kaladinDone")
  case InvitedToStudy(invitedBy: UserId, studyName: StudyName, studyId: StudyId)
      extends NotificationContent("invitedStudy")
  case TeamJoined(id: TeamId, name: String) extends NotificationContent("teamJoined")
  case MentionedInThread(
      mentionedBy: UserId,
      topicName: String,
      topidId: ForumTopicId,
      category: ForumCategId,
      postId: ForumPostId
  ) extends NotificationContent("mention")
  case PrivateMessage(user: UserId, text: String) extends NotificationContent("privateMessage")
  case GenericLink(
      url: String,
      title: Option[String],
      text: Option[String],
      icon: String // should be lila.ui.Icon
  ) extends NotificationContent("genericLink")
  case ReportedBanned extends NotificationContent("reportedBanned")
  case RatingRefund(perf: String, points: Int) extends NotificationContent("ratingRefund")
  case GameEnd(gameId: GameFullId, opponentId: Option[UserId], win: Option[Boolean])
      extends NotificationContent("gameEnd")
  case StreamStart(streamerId: UserId, streamerName: String) extends NotificationContent("streamStart")
  case BroadcastRound(url: String, title: String, text: String) extends NotificationContent("broadcastRound")
  case TitledTournamentInvitation(id: TourId, text: String) extends NotificationContent("titledTourney")
  case CoachReview extends NotificationContent("coachReview")
  case PlanStart(userId: UserId) extends NotificationContent("planStart") // BC
  case PlanExpire(userId: UserId) extends NotificationContent("planExpire") // BC
  case CorresAlarm(gameId: GameId, opponent: String) extends NotificationContent("corresAlarm")
  case Recap(year: Int) extends NotificationContent("recap")

case class NotifyAllows(userId: UserId, allows: Allows)
case class PushNotification(
    to: Iterable[NotifyAllows],
    content: NotificationContent,
    params: Iterable[(String, String)] = Nil
)

enum PrefEvent:
  case privateMessage
  case challenge
  case mention
  case streamStart
  case tournamentSoon
  case gameEvent
  case invitedStudy
  case broadcastRound
  def key = toString

opaque type Allows = Int
object Allows extends OpaqueInt[Allows]:
  import NotificationPref.*
  extension (e: Allows)
    def push: Boolean = (e.value & PUSH) != 0
    def web: Boolean = (e.value & WEB) != 0
    def device: Boolean = (e.value & DEVICE) != 0
    def bell: Boolean = (e.value & BELL) != 0
    def any: Boolean = e.value != 0

object NotificationPref:
  val BELL = 1
  val WEB = 2
  val DEVICE = 4
  val PUSH = WEB | DEVICE

private type GetNotifyAllowsType = (UserId, PrefEvent) => Fu[Allows]
opaque type GetNotifyAllows <: GetNotifyAllowsType = GetNotifyAllowsType
object GetNotifyAllows extends TotalWrapper[GetNotifyAllows, GetNotifyAllowsType]

abstract class NotifyApi(val prefColl: reactivemongo.api.bson.collection.BSONCollection):
  import reactivemongo.api.bson.BSONDocument
  def notifyOne[U: UserIdOf](to: U, content: NotificationContent): Funit
  def notifyMany(userIds: Iterable[UserId], content: NotificationContent): Funit
  def markRead(to: UserId, selector: BSONDocument): Funit
  def remove(to: UserId, selector: BSONDocument): Funit
