package lila.core
package notify

import alleycats.Zero

import lila.core.chess.Win
import lila.core.id.*
import lila.core.study.data.StudyName
import lila.core.userId.*

opaque type UnreadCount = Int
object UnreadCount extends OpaqueInt[UnreadCount]:
  given Zero[UnreadCount] = Zero(0)

abstract class NotificationContent(val key: String)

case class NotifiedBatch(userIds: Iterable[UserId])

case class IrwinDone(userId: UserId)   extends NotificationContent("irwinDone")
case class KaladinDone(userId: UserId) extends NotificationContent("kaladinDone")
case class InvitedToStudy(invitedBy: UserId, studyName: StudyName, studyId: StudyId)
    extends NotificationContent("invitedStudy")
case class TeamJoined(id: TeamId, name: String) extends NotificationContent("teamJoined")
case class MentionedInThread(
    mentionedBy: UserId,
    topicName: String,
    topidId: ForumTopicId,
    category: ForumCategId,
    postId: ForumPostId
) extends NotificationContent("mention")
case class PrivateMessage(user: UserId, text: String) extends NotificationContent("privateMessage")
case class GenericLink(
    url: String,
    title: Option[String],
    text: Option[String],
    icon: String // should be lila.ui.Icon
) extends NotificationContent("genericLink")
case object ReportedBanned                         extends NotificationContent("reportedBanned")
case class RatingRefund(perf: String, points: Int) extends NotificationContent("ratingRefund")
case class GameEnd(gameId: GameFullId, opponentId: Option[UserId], win: Option[Win])
    extends NotificationContent("gameEnd")
case class StreamStart(streamerId: UserId, streamerName: String) extends NotificationContent("streamStart")
case class BroadcastRound(url: String, title: String, text: String)
    extends NotificationContent("broadcastRound")

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
    def push: Boolean   = (e.value & PUSH) != 0
    def web: Boolean    = (e.value & WEB) != 0
    def device: Boolean = (e.value & DEVICE) != 0
    def bell: Boolean   = (e.value & BELL) != 0
    def any: Boolean    = e.value != 0

object NotificationPref:
  val BELL   = 1
  val WEB    = 2
  val DEVICE = 4
  val PUSH   = WEB | DEVICE

private type GetNotifyAllowsType                   = (UserId, PrefEvent) => Fu[Allows]
opaque type GetNotifyAllows <: GetNotifyAllowsType = GetNotifyAllowsType
object GetNotifyAllows extends TotalWrapper[GetNotifyAllows, GetNotifyAllowsType]

abstract class NotifyApi(val prefColl: reactivemongo.api.bson.collection.BSONCollection):
  import reactivemongo.api.bson.BSONDocument
  def notifyOne[U: UserIdOf](to: U, content: NotificationContent): Funit
  def notifyMany(userIds: Iterable[UserId], content: NotificationContent): Funit
  def markRead(to: UserId, selector: BSONDocument): Funit
  def remove(to: UserId, selector: BSONDocument): Funit
