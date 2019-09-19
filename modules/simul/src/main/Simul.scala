package lila.simul

import chess.variant.Variant
import lila.user.{ User, Title }
import org.joda.time.DateTime
import ornicar.scalalib.Random

case class Simul(
    _id: Simul.ID,
    name: String,
    status: SimulStatus,
    clock: SimulClock,
    applicants: List[SimulApplicant],
    pairings: List[SimulPairing],
    variants: List[Variant],
    createdAt: DateTime,
    hostId: String,
    hostRating: Int,
    hostTitle: Option[Title],
    hostGameId: Option[String], // game the host is focusing on
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime],
    hostSeenAt: Option[DateTime],
    color: Option[String],
    text: String,
    team: Option[String]
) {
  def id = _id

  def fullName = s"$name simul"

  def isCreated = !isStarted

  def isStarted = startedAt.isDefined

  def isFinished = status == SimulStatus.Finished

  def isRunning = status == SimulStatus.Started

  def hasApplicant(userId: String) = applicants.exists(_ is userId)

  def hasPairing(userId: String) = pairings.exists(_ is userId)

  def hasUser(userId: String) = hasApplicant(userId) || hasPairing(userId)

  def addApplicant(applicant: SimulApplicant) = Created {
    if (!hasApplicant(applicant.player.user) && variants.has(applicant.player.variant))
      copy(applicants = applicants :+ applicant)
    else this
  }

  def removeApplicant(userId: String) = Created {
    copy(applicants = applicants filterNot (_ is userId))
  }

  def accept(userId: String, v: Boolean) = Created {
    copy(applicants = applicants map {
      case a if a is userId => a.copy(accepted = v)
      case a => a
    })
  }

  def removePairing(userId: String) =
    copy(pairings = pairings filterNot (_ is userId)).finishIfDone

  def nbAccepted = applicants.count(_.accepted)

  def startable = isCreated && nbAccepted > 1

  def start = startable option copy(
    status = SimulStatus.Started,
    startedAt = DateTime.now.some,
    applicants = Nil,
    pairings = applicants collect {
      case a if a.accepted => SimulPairing(a.player)
    },
    hostSeenAt = none
  )

  def updatePairing(gameId: String, f: SimulPairing => SimulPairing) = copy(
    pairings = pairings collect {
      case p if p.gameId == gameId => f(p)
      case p => p
    }
  ).finishIfDone

  def ejectCheater(userId: String): Option[Simul] =
    hasUser(userId) option removeApplicant(userId).removePairing(userId)

  private def finishIfDone =
    if (pairings.forall(_.finished))
      copy(
        status = SimulStatus.Finished,
        finishedAt = DateTime.now.some,
        hostGameId = none
      )
    else this

  def gameIds = pairings.map(_.gameId)

  def perfTypes: List[lila.rating.PerfType] = variants.flatMap { variant =>
    lila.game.PerfPicker.perfType(
      speed = chess.Speed(clock.config.some),
      variant = variant,
      daysPerTurn = none
    )
  }

  def applicantRatio = s"${applicants.count(_.accepted)}/${applicants.size}"

  def variantRich = variants.size > 3

  def isHost(userOption: Option[User]): Boolean = userOption ?? isHost
  def isHost(user: User): Boolean = user.id == hostId

  def playingPairings = pairings filterNot (_.finished)

  def hostColor = (color flatMap chess.Color.apply) | chess.Color(scala.util.Random.nextBoolean)

  def setPairingHostColor(gameId: String, hostColor: chess.Color) =
    updatePairing(gameId, _.copy(hostColor = hostColor))

  def isNotBrandNew = createdAt isBefore DateTime.now.minusSeconds(10)

  private def Created(s: => Simul): Simul = if (isCreated) s else this

  def spotlightable =
    (hostRating >= 2400 || hostTitle.isDefined) &&
      isCreated &&
      applicants.size < 80

  def wins = pairings.count(p => p.finished && p.wins.has(false))
  def draws = pairings.count(p => p.finished && p.wins.isEmpty)
  def losses = pairings.count(p => p.finished && p.wins.has(true))
  def ongoing = pairings.count(_.ongoing)
}

object Simul {

  type ID = String

  case class OnStart(simul: Simul)

  private def makeName(host: User) =
    if (host.title.isDefined) host.titleUsername
    else RandomName()

  def make(
    host: User,
    clock: SimulClock,
    variants: List[Variant],
    color: String,
    text: String,
    team: Option[String]
  ): Simul = Simul(
    _id = Random nextString 8,
    name = makeName(host),
    status = SimulStatus.Created,
    clock = clock,
    hostId = host.id,
    hostRating = host.perfs.bestRatingIn {
      variants flatMap { variant =>
        lila.game.PerfPicker.perfType(
          speed = chess.Speed(clock.config.some),
          variant = variant,
          daysPerTurn = none
        )
      }
    },
    hostTitle = host.title,
    hostGameId = none,
    createdAt = DateTime.now,
    variants = variants,
    applicants = Nil,
    pairings = Nil,
    startedAt = none,
    finishedAt = none,
    hostSeenAt = DateTime.now.some,
    color = color.some,
    text = text,
    team = team
  )
}
