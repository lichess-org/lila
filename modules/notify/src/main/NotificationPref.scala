package lila.notify

import reactivemongo.api.bson.*

import lila.core.notify.{ Allows, PrefEvent }
import lila.core.notify as core

object Allows:
  import NotificationPref.*

  def fromForm(bell: Boolean, push: Boolean): Allows =
    core.Allows((bell.so(BELL)) | (push.so(PUSH)))

  def toForm(allows: Allows): Some[(Boolean, Boolean)] =
    Some((allows.bell, allows.push))

  val all = core.Allows(BELL | WEB | DEVICE | PUSH)

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
  def allows(event: PrefEvent): Allows = event match
    case PrefEvent.privateMessage => privateMessage
    case PrefEvent.challenge      => challenge
    case PrefEvent.mention        => mention
    case PrefEvent.streamStart    => streamStart
    case PrefEvent.tournamentSoon => tournamentSoon
    case PrefEvent.gameEvent      => gameEvent
    case PrefEvent.invitedStudy   => invitedStudy
    case PrefEvent.broadcastRound => broadcastRound

object NotificationPref:
  export lila.core.notify.NotificationPref.*

  val events = PrefEvent.values.mapBy(_.key)

  val default: NotificationPref = NotificationPref(
    privateMessage = core.Allows(BELL | PUSH),
    challenge = core.Allows(BELL | PUSH),
    mention = core.Allows(BELL | PUSH),
    streamStart = core.Allows(BELL | PUSH),
    tournamentSoon = core.Allows(PUSH),
    gameEvent = core.Allows(PUSH),
    invitedStudy = core.Allows(BELL | PUSH),
    broadcastRound = core.Allows(BELL | PUSH),
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

  import lila.db.dsl.given
  given BSONDocumentHandler[NotificationPref] = Macros.handler

  import play.api.libs.json.{ Json, Writes, OWrites }

  given OWrites[NotificationPref] = Json.writes[NotificationPref]

  private given Writes[Allows] = Writes { a =>
    Json.toJson(List(BELL -> "bell", PUSH -> "push").collect {
      case (tpe, str) if (a.value & tpe) != 0 => str
    })
  }
