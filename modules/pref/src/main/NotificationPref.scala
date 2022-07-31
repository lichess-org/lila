package lila.pref

import play.api.libs.json.{ Json, OWrites }
import reactivemongo.api.bson.{ BSONDocumentHandler, Macros }

case class NotificationPref(
    inboxMsg: Int,
    challenge: Int,
    gameEvent: Int,
    tournamentSoon: Int,
    timeAlarm: Int,
    streamStart: Int,
    forumMention: Int,
    correspondenceEmail: Int
) {

  import NotificationPref._

  def filter(notification: Type): Int =
    notification match {
      case InboxMsg       => inboxMsg
      case Challenge      => challenge
      case GameEvent      => gameEvent
      case TournamentSoon => tournamentSoon
      case TimeAlarm      => timeAlarm
      case StreamStart    => streamStart
      case ForumMention   => forumMention
    }
}

object NotificationPref {
  val NONE            = 0
  val BELL            = 1
  val WEB             = 2 // web push
  val BELL_AND_WEB    = BELL | WEB
  val DEVICE          = 4 // firebase
  val BELL_AND_DEVICE = BELL | DEVICE
  val WEB_AND_DEVICE  = WEB | DEVICE
  val ALL             = BELL | WEB | DEVICE

  val choices: Seq[Int]     = Seq(NONE, WEB, DEVICE, WEB_AND_DEVICE)        // non-bell push notifications
  val moreChoices: Seq[Int] = Seq(BELL, BELL_AND_WEB, BELL_AND_DEVICE, ALL) // inbox
  val noneMoreChoices: Seq[Int] = NONE +: moreChoices // stream start & forum mention

  sealed trait Type {
    override def toString: String = { // to match db fields
      val typeName = getClass.getSimpleName
      s"${typeName.charAt(0).toLower}${typeName.substring(1, typeName.length - 1)}" // strip object class $
    }
  }

  case object InboxMsg       extends Type
  case object GameEvent      extends Type
  case object Challenge      extends Type
  case object TournamentSoon extends Type
  case object TimeAlarm      extends Type
  case object StreamStart    extends Type
  case object ForumMention   extends Type

  lazy val default: NotificationPref = NotificationPref(
    inboxMsg = ALL,
    challenge = WEB_AND_DEVICE,
    gameEvent = WEB_AND_DEVICE,
    tournamentSoon = WEB_AND_DEVICE,
    timeAlarm = WEB_AND_DEVICE,
    streamStart = NONE,
    forumMention = BELL,
    correspondenceEmail = 0
  )
  /* example within a pref json view:
    ...
    "notification": {
      "correspondenceEmail": true,
      "streamStart": "bell|web|device",
      "gameEvent": "web",
      "inboxMsg": "",
      ...
   */
  implicit val notificationDataJsonWriter: OWrites[NotificationPref] =
    OWrites[NotificationPref] { ndata =>
      Json.obj(
        "inboxMsg"            -> filterToString(ndata.inboxMsg),
        "challenge"           -> filterToString(ndata.challenge),
        "gameEvent"           -> filterToString(ndata.gameEvent),
        "tournamentSoon"      -> filterToString(ndata.tournamentSoon),
        "timeAlarm"           -> filterToString(ndata.timeAlarm),
        "streamStart"         -> filterToString(ndata.streamStart),
        "forumMention"        -> filterToString(ndata.forumMention),
        "correspondenceEmail" -> (ndata.correspondenceEmail != 0)
      )
    }

  private def filterToString(filter: Int): String = (
    Map(BELL -> "bell", WEB -> "web", DEVICE -> "device") collect {
      case (tpe, str) if (filter & tpe) != 0 => str
    }
  ).mkString("|")

  implicit val NotificationDataBSONHandler: BSONDocumentHandler[NotificationPref] =
    Macros.handler[NotificationPref]
}
