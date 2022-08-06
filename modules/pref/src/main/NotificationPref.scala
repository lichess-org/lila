package lila.pref

import play.api.libs.json.{ Json, OWrites }
import reactivemongo.api.bson.Macros
import NotificationPref._

case class NotificationPref(
    inboxMsg: Allows,
    challenge: Allows,
    forumMention: Allows,
    streamStart: Allows,
    tournamentSoon: Allows,
    gameEvent: Allows,
    correspondenceEmail: Int
) {

  def allows(event: Event): Allows = event match {
    case InboxMsg => inboxMsg
    case Challenge => challenge
    case ForumMention => forumMention
    case StreamStart => streamStart
    case TournamentSoon => tournamentSoon
    case GameEvent => gameEvent
  }
}

object NotificationPref {
  val BELL            = 1
  val WEB             = 2 // web push & browser alerts from notify/*.ts
  val DEVICE          = 4 // firebase
  val PUSH            = WEB|DEVICE  // may need to separate when macOS ventura happens

  case class Allows(value: Int) extends AnyVal with IntValue {
    def push: Boolean = (value & (WEB|DEVICE)) != 0
    def web: Boolean = (value & WEB) != 0
    def device: Boolean = (value & DEVICE) != 0
    def bell: Boolean = (value & BELL) != 0
    def any: Boolean = value != 0
  }

  sealed trait Event {
    override def toString: String = { // camelcase for matching db fields
      val typeName = getClass.getSimpleName
      s"${typeName.charAt(0).toLower}${typeName.substring(1, typeName.length - 1)}" // strip $
    }
  }

  case object InboxMsg       extends Event
  case object Challenge      extends Event
  case object ForumMention   extends Event
  case object StreamStart    extends Event
  case object TournamentSoon extends Event
  case object GameEvent      extends Event

  lazy val default: NotificationPref = NotificationPref(
    inboxMsg = Allows(BELL|PUSH),
    challenge = Allows(BELL|PUSH),
    forumMention = Allows(BELL),
    streamStart = Allows(0),
    tournamentSoon = Allows(PUSH),
    gameEvent = Allows(PUSH),
    correspondenceEmail = 0
  )

  implicit private val AllowsBSONHandler =
    lila.db.dsl.intAnyValHandler[Allows](_.value, Allows.apply)

  implicit val NotificationPrefBSONHandler =
    Macros.handler[NotificationPref]

  object Allows {

    def fromForm(bell: Boolean, push: Boolean): Allows =
      Allows((bell ?? BELL)|(push ?? PUSH))

    def toForm(allows: Allows): Some[(Boolean, Boolean)] =
      Some((allows.bell, allows.push))
  }

  implicit val notificationDataJsonWriter: OWrites[NotificationPref] =
    OWrites[NotificationPref] { data =>
      Json.obj(
        "inboxMsg"            -> allowsToJson(data.inboxMsg),
        "forumMention"        -> allowsToJson(data.forumMention),
        "streamStart"         -> allowsToJson(data.streamStart),
        "challenge"           -> allowsToJson(data.challenge),
        "tournamentSoon"      -> allowsToJson(data.tournamentSoon),
        "gameEvent"           -> allowsToJson(data.gameEvent),
        "correspondenceEmail" -> (data.correspondenceEmail != 0)
      )
    }

  private def allowsToJson(v: Allows) = List(
    Map(BELL -> "bell", PUSH -> "push") collect {
      case (tpe, str) if (v.value & tpe) != 0 => str
    }
  )
}
