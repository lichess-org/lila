package lila.simul

import chess.variant.Variant
import lila.user.User
import org.joda.time.{ DateTime, Duration }
import ornicar.scalalib.Random

case class Simul(
    _id: Simul.ID,
    name: String,
    clock: SimulClock,
    applicants: List[SimulApplicant],
    pairings: List[SimulPairing],
    variants: List[Variant],
    createdAt: DateTime,
    hostId: String,
    hostRating: Int,
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime]) {

  def id = _id

  def isStarted = startedAt.isDefined

  def isOpen = !isStarted

  def isFinished = finishedAt.isDefined

  def hasApplicant(userId: String) = applicants.exists(_ is userId)

  def hasPairing(userId: String) = pairings.exists(_ is userId)

  def hasUser(userId: String) = hasApplicant(userId) || hasPairing(userId)

  def addApplicant(applicant: SimulApplicant) = Open {
    if (!hasApplicant(applicant.player.user) && variants.contains(applicant.player.variant))
      copy(applicants = applicants :+ applicant)
    else this
  }

  def removeApplicant(userId: String) = Open {
    copy(applicants = applicants filterNot (_ is userId))
  }

  def accept(userId: String, v: Boolean) = Open {
    copy(applicants = applicants map {
      case a if a is userId => a.copy(accepted = v)
      case a                => a
    })
  }

  def removePairing(userId: String) =
    copy(pairings = pairings filterNot (_ is userId))

  def startable = isOpen && applicants.count(_.accepted) > 1

  def start = startable option copy(
    startedAt = DateTime.now.some,
    applicants = Nil,
    pairings = applicants collect {
      case a if a.accepted => SimulPairing(a.player)
    })

  def updatePairing(gameId: String, f: SimulPairing => SimulPairing) = copy(
    pairings = pairings collect {
      case p if p.gameId == gameId => f(p)
      case p                       => p
    })

  def ejectCheater(userId: String): Option[Simul] =
    hasUser(userId) option removeApplicant(userId).removePairing(userId)

  def finish = copy(finishedAt = DateTime.now.some)

  def gameIds = pairings.map(_.gameId)

  private def Open(s: => Simul): Simul = if (isOpen) s else this
}

object Simul {

  type ID = String

  def make(
    name: String,
    host: User,
    clock: SimulClock,
    variants: List[Variant]): Simul = Simul(
    _id = Random nextStringUppercase 8,
    name = name,
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
    createdAt = DateTime.now,
    variants = variants,
    applicants = Nil,
    pairings = Nil,
    startedAt = none,
    finishedAt = none)
}
