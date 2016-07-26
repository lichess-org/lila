package lila.simul

import chess.variant.Variant
import lila.user.User
import org.joda.time.{ DateTime, Duration }
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
    hostGameId: Option[String], // game the host is focusing on
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime],
    hostSeenAt: Option[DateTime],
    color: Option[String]) {

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
    if (!hasApplicant(applicant.player.user) && variants.contains(applicant.player.variant))
      copy(applicants = applicants :+ applicant)
    else this
  }

  def removeApplicant(userId: String) = Created {
    copy(applicants = applicants filterNot (_ is userId))
  }

  def accept(userId: String, v: Boolean) = Created {
    copy(applicants = applicants map {
      case a if a is userId => a.copy(accepted = v)
      case a                => a
    })
  }

  def removePairing(userId: String) =
    copy(pairings = pairings filterNot (_ is userId)).finishIfDone

  def startable = isCreated && applicants.count(_.accepted) > 1

  def start = startable option copy(
    status = SimulStatus.Started,
    startedAt = DateTime.now.some,
    applicants = Nil,
    pairings = applicants collect {
      case a if a.accepted => SimulPairing(a.player)
    },
    hostSeenAt = none)

  def updatePairing(gameId: String, f: SimulPairing => SimulPairing) = copy(
    pairings = pairings collect {
      case p if p.gameId == gameId => f(p)
      case p                       => p
    }).finishIfDone

  def ejectCheater(userId: String): Option[Simul] =
    hasUser(userId) option removeApplicant(userId).removePairing(userId)

  private def finishIfDone =
    if (pairings.forall(_.finished))
      copy(
        status = SimulStatus.Finished,
        finishedAt = DateTime.now.some,
        hostGameId = none)
    else this

  def gameIds = pairings.map(_.gameId)

  def perfTypes: List[lila.rating.PerfType] = variants.flatMap { variant =>
    lila.game.PerfPicker.perfType(
      speed = chess.Speed(clock.chessClock.some),
      variant = variant,
      daysPerTurn = none)
  }

  def applicantRatio = s"${applicants.count(_.accepted)}/${applicants.size}"

  def variantRich = variants.size > 3

  def isHost(userOption: Option[User]) = userOption ?? (_.id == hostId)

  def playingPairings = pairings filterNot (_.finished)

  def hostColor = (color flatMap chess.Color.apply) | chess.Color(scala.util.Random.nextBoolean)

  def setPairingHostColor(gameId: String, hostColor: chess.Color) =
    updatePairing(gameId, _.copy(hostColor = hostColor))

  def isNotBrandNew = createdAt isBefore DateTime.now.minusSeconds(10)

  private def Created(s: => Simul): Simul = if (isCreated) s else this

  def spotlightable = isCreated && hostRating >= 2400
}

object Simul {

  type ID = String

  def make(
    host: User,
    clock: SimulClock,
    variants: List[Variant],
    color: String): Simul = Simul(
    _id = Random nextStringUppercase 8,
    name = RandomName(),
    status = SimulStatus.Created,
    clock = clock,
    hostId = host.id,
    hostRating = host.perfs.bestRatingIn {
      variants flatMap { variant =>
        lila.game.PerfPicker.perfType(
          speed = chess.Speed(clock.chessClock.some),
          variant = variant,
          daysPerTurn = none)
      }
    },
    hostGameId = none,
    createdAt = DateTime.now,
    variants = variants,
    applicants = Nil,
    pairings = Nil,
    startedAt = none,
    finishedAt = none,
    hostSeenAt = DateTime.now.some,
    color = color.some)
}
