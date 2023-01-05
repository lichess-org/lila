package lila.notify

import reactivemongo.api.bson.*
import NotificationPref.*
import alleycats.Zero

// #TODO opaque type
case class Allows(value: Int) extends AnyVal with IntValue:
  def push: Boolean   = (value & NotificationPref.PUSH) != 0
  def web: Boolean    = (value & NotificationPref.WEB) != 0
  def device: Boolean = (value & NotificationPref.DEVICE) != 0
  def bell: Boolean   = (value & NotificationPref.BELL) != 0
  def any: Boolean    = value != 0

object Allows:
  given Zero[Allows] = Zero(Allows(0))

  def fromForm(bell: Boolean, push: Boolean): Allows =
    Allows((bell ?? BELL) | (push ?? PUSH))

  def toForm(allows: Allows): Some[(Boolean, Boolean)] =
    Some((allows.bell, allows.push))

  def fromCode(code: Int) = Allows(code)

case class NotifyAllows(userId: UserId, allows: Allows):
  export allows.*

// take care with NotificationPref field names - they map directly to db and ws channels

case class NotificationPref(
    privateMessage: Allows,
    challenge: Allows,
    mention: Allows,
    streamStart: Allows,
    tournamentSoon: Allows,
    gameEvent: Allows,
    invitedStudy: Allows,
    correspondenceEmail: Boolean
):
  // def allows(key: String): Allows =
  //   NotificationPref.Event.byKey.get(key) ?? allows
  def allows(event: Event): Allows = event match
    case PrivateMessage => privateMessage
    case Challenge      => challenge
    case Mention        => mention
    case StreamStart    => streamStart
    case TournamentSoon => tournamentSoon
    case GameEvent      => gameEvent
    case InvitedStudy   => invitedStudy

object NotificationPref:
  val BELL   = 1
  val WEB    = 2
  val DEVICE = 4
  val PUSH   = WEB | DEVICE

  enum Event:
    case PrivateMessage
    case Challenge
    case Mention
    case StreamStart
    case TournamentSoon
    case GameEvent
    case InvitedStudy

    def key = lila.common.String.lcfirst(this.toString)

  object Event:
    val byKey = values.mapBy(_.key)

  export Event.*

  val default: NotificationPref = NotificationPref(
    privateMessage = Allows(BELL | PUSH),
    challenge = Allows(BELL | PUSH),
    mention = Allows(BELL | PUSH),
    streamStart = Allows(BELL | PUSH),
    tournamentSoon = Allows(PUSH),
    gameEvent = Allows(PUSH),
    invitedStudy = Allows(BELL | PUSH),
    correspondenceEmail = false
  )

  object form:
    import play.api.data.*
    import play.api.data.Forms.*

    private val allowsMapping =
      mapping("bell" -> boolean, "push" -> boolean)(Allows.fromForm)(Allows.toForm)

    val form = Form(
      mapping(
        "privateMessage"      -> allowsMapping,
        "challenge"           -> allowsMapping,
        "mention"             -> allowsMapping,
        "streamStart"         -> allowsMapping,
        "tournamentSoon"      -> allowsMapping,
        "gameEvent"           -> allowsMapping,
        "invitedStudy"        -> allowsMapping,
        "correspondenceEmail" -> boolean
      )(NotificationPref.apply)(lila.notify.unapply)
    )

  given BSONHandler[Allows] =
    lila.db.dsl.intAnyValHandler[Allows](_.value, Allows.apply)

  given BSONDocumentHandler[NotificationPref] = Macros.handler

  import play.api.libs.json.{ Json, Writes, OWrites }

  given OWrites[NotificationPref] = Json.writes[NotificationPref]

  private given Writes[Allows] = Writes { a =>
    Json.toJson(List(BELL -> "bell", PUSH -> "push") collect {
      case (tpe, str) if (a.value & tpe) != 0 => str
    })
  }
