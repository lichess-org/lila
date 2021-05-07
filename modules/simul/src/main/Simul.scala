package lila.simul

import chess.Color
import chess.format.FEN
import chess.variant.Variant
import chess.{ Speed }
import org.joda.time.DateTime

import lila.rating.PerfType
import lila.user.User

case class Simul(
    _id: Simul.ID,
    name: String,
    status: SimulStatus,
    clock: SimulClock,
    applicants: List[SimulApplicant],
    pairings: List[SimulPairing],
    variants: List[Variant],
    position: Option[FEN],
    createdAt: DateTime,
    hostId: User.ID,
    hostRating: Int,
    hostGameId: Option[String], // game the host is focusing on
    startedAt: Option[DateTime],
    finishedAt: Option[DateTime],
    hostSeenAt: Option[DateTime],
    color: Option[String],
    text: String,
    team: Option[String],
    featurable: Option[Boolean]
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

  def addApplicant(applicant: SimulApplicant) =
    Created {
      if (!hasApplicant(applicant.player.user) && variants.has(applicant.player.variant))
        copy(applicants = applicants :+ applicant)
      else this
    }

  def removeApplicant(userId: String) =
    Created {
      copy(applicants = applicants.filterNot(_ is userId))
    }

  def accept(userId: String, v: Boolean) =
    Created {
      copy(applicants = applicants map {
        case a if a is userId => a.copy(accepted = v)
        case a                => a
      })
    }

  def removePairing(userId: String) =
    copy(pairings = pairings.filterNot(_ is userId)).finishIfDone

  def nbAccepted = applicants.count(_.accepted)

  def startable = isCreated && nbAccepted > 1

  def start =
    startable option copy(
      status = SimulStatus.Started,
      startedAt = DateTime.now.some,
      applicants = Nil,
      pairings = applicants collect {
        case a if a.accepted => SimulPairing(a.player)
      },
      hostSeenAt = none
    )

  def updatePairing(gameId: String, f: SimulPairing => SimulPairing) =
    copy(
      pairings = pairings collect {
        case p if p.gameId == gameId => f(p)
        case p                       => p
      }
    ).finishIfDone

  def ejectCheater(userId: String): Option[Simul] =
    hasUser(userId) option removeApplicant(userId).removePairing(userId)

  private def finishIfDone =
    if (isStarted && pairings.forall(_.finished))
      copy(
        status = SimulStatus.Finished,
        finishedAt = DateTime.now.some,
        hostGameId = none
      )
    else this

  def gameIds = pairings.map(_.gameId)

  def perfTypes: List[lila.rating.PerfType] =
    variants.flatMap { variant =>
      lila.game.PerfPicker.perfType(
        speed = Speed(clock.config.some),
        variant = variant,
        daysPerTurn = none
      )
    }

  def applicantRatio = s"${applicants.count(_.accepted)}/${applicants.size}"

  def variantRich = variants.sizeIs > 3

  def isHost(userOption: Option[User]): Boolean = userOption ?? isHost
  def isHost(user: User): Boolean               = user.id == hostId

  def playingPairings = pairings filterNot (_.finished)

  def hostColor: Option[Color] = color flatMap chess.Color.fromName

  def setPairingHostColor(gameId: String, hostColor: chess.Color) =
    updatePairing(gameId, _.copy(hostColor = hostColor))

  private def Created(s: => Simul): Simul = if (isCreated) s else this

  def wins    = pairings.count(p => p.finished && p.wins.has(false))
  def draws   = pairings.count(p => p.finished && p.wins.isEmpty)
  def losses  = pairings.count(p => p.finished && p.wins.has(true))
  def ongoing = pairings.count(_.ongoing)
}

object Simul {

  type ID = String

  case class OnStart(simul: Simul) extends AnyVal

  def make(
      host: User,
      name: String,
      clock: SimulClock,
      variants: List[Variant],
      position: Option[FEN],
      color: String,
      text: String,
      team: Option[String],
      featurable: Option[Boolean]
  ): Simul =
    Simul(
      _id = lila.common.ThreadLocalRandom nextString 8,
      name = name,
      status = SimulStatus.Created,
      clock = clock,
      hostId = host.id,
      hostRating = host.perfs.bestRatingIn {
        variants.flatMap { variant =>
          lila.game.PerfPicker.perfType(
            speed = Speed(clock.config.some),
            variant = variant,
            daysPerTurn = none
          )
        } ::: List(PerfType.Blitz, PerfType.Rapid, PerfType.Classical)
      },
      hostGameId = none,
      createdAt = DateTime.now,
      variants = if (position.isDefined) List(chess.variant.Standard) else variants,
      position = position,
      applicants = Nil,
      pairings = Nil,
      startedAt = none,
      finishedAt = none,
      hostSeenAt = DateTime.now.some,
      color = color.some,
      text = text,
      team = team,
      featurable = featurable
    )
}
