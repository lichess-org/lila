package lila.notify

import alleycats.Zero
import reactivemongo.api.bson.*

import NotificationPref.*

opaque type Allows = Int
object Allows extends OpaqueInt[Allows]:
  extension (e: Allows)
    def push: Boolean   = (e & PUSH) != 0
    def web: Boolean    = (e & WEB) != 0
    def device: Boolean = (e & DEVICE) != 0
    def bell: Boolean   = (e & BELL) != 0
    def any: Boolean    = e != 0

  given Zero[Allows] = Zero(Allows(0))

  def fromForm(bell: Boolean, push: Boolean): Allows =
    Allows((bell.so(BELL)) | (push.so(PUSH)))

  def toForm(allows: Allows): Some[(Boolean, Boolean)] =
    Some((allows.bell, allows.push))

  val all = Allows(BELL | WEB | DEVICE | PUSH)

case class NotifyAllows(userId: UserId, allows: Allows)

case class NotificationPref(
    privateMessage: Allows,
    challenge: Allows,
    mention: Allows,
    streamStart: Allows,
    tournamentSoon: Allows,
    gameEvent: Allows,
    invitedStudy: Allows,
    broadcastRound: Allows = NotificationPref.default.broadcastRound,
    correspondenceEmail: Boolean
):
  // def allows(key: String): Allows =
  //   NotificationPref.Event.byKey.get(key) so allows
  def allows(event: Event): Allows = event match
    case PrivateMessage => privateMessage
    case Challenge      => challenge
    case Mention        => mention
    case StreamStart    => streamStart
    case TournamentSoon => tournamentSoon
    case GameEvent      => gameEvent
    case InvitedStudy   => invitedStudy
    case BroadcastRound => broadcastRound

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
    case BroadcastRound

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
    broadcastRound = Allows(BELL | PUSH),
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
        "broadcastRound"      -> allowsMapping,
        "correspondenceEmail" -> boolean
      )(NotificationPref.apply)(lila.notify.unapply)
    )

  import lila.db.dsl.opaqueHandler
  given BSONDocumentHandler[NotificationPref] = Macros.handler

  import play.api.libs.json.{ Json, Writes, OWrites }

  given OWrites[NotificationPref] = Json.writes[NotificationPref]

  private given Writes[Allows] = Writes { a =>
    Json.toJson(List(BELL -> "bell", PUSH -> "push").collect {
      case (tpe, str) if (a.value & tpe) != 0 => str
    })
  }
