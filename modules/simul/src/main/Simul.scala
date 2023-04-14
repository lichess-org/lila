package lila.simul

import ornicar.scalalib.ThreadLocalRandom
import chess.Color
import chess.format.Fen
import chess.variant.Variant
import chess.Speed

import lila.rating.PerfType
import lila.user.User

case class Simul(
    _id: SimulId,
    name: String,
    status: SimulStatus,
    clock: SimulClock,
    applicants: List[SimulApplicant],
    pairings: List[SimulPairing],
    variants: List[Variant],
    position: Option[Fen.Epd],
    createdAt: Instant,
    estimatedStartAt: Option[Instant] = None,
    hostId: UserId,
    hostRating: IntRating,
    hostGameId: Option[String], // game the host is focusing on
    startedAt: Option[Instant],
    finishedAt: Option[Instant],
    hostSeenAt: Option[Instant],
    color: Option[String],
    text: String,
    team: Option[TeamId],
    featurable: Option[Boolean]
):
  inline def id = _id

  def fullName = s"$name simul"

  def isCreated = !isStarted

  def isStarted = startedAt.isDefined

  def isFinished = status == SimulStatus.Finished

  def isRunning = status == SimulStatus.Started

  def hasApplicant(userId: UserId) = applicants.exists(_ is userId)

  def hasPairing(userId: UserId) = pairings.exists(_ is userId)

  def hasUser(userId: UserId) = hasApplicant(userId) || hasPairing(userId)

  def addApplicant(applicant: SimulApplicant) =
    Created {
      if (!hasApplicant(applicant.player.user) && variants.has(applicant.player.variant))
        copy(applicants = applicants :+ applicant)
      else this
    }

  def removeApplicant(userId: UserId) =
    Created {
      copy(applicants = applicants.filterNot(_ is userId))
    }

  def accept(userId: UserId, v: Boolean) =
    Created {
      copy(applicants = applicants map {
        case a if a is userId => a.copy(accepted = v)
        case a                => a
      })
    }

  def removePairing(userId: UserId) =
    copy(pairings = pairings.filterNot(_ is userId)).finishIfDone

  def nbAccepted = applicants.count(_.accepted)

  def startable = isCreated && nbAccepted > 1

  def start =
    startable option copy(
      status = SimulStatus.Started,
      startedAt = nowInstant.some,
      applicants = Nil,
      clock = clock.adjustedForPlayers(nbAccepted),
      pairings = applicants collect {
        case a if a.accepted => SimulPairing(a.player)
      },
      hostSeenAt = none
    )

  def updatePairing(gameId: GameId, f: SimulPairing => SimulPairing) =
    copy(
      pairings = pairings collect {
        case p if p.gameId == gameId => f(p)
        case p                       => p
      }
    ).finishIfDone

  def ejectCheater(userId: UserId): Option[Simul] =
    hasUser(userId) option removeApplicant(userId).removePairing(userId)

  private def finishIfDone =
    if (isStarted && pairings.forall(_.finished))
      copy(
        status = SimulStatus.Finished,
        finishedAt = nowInstant.some,
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

  def setPairingHostColor(gameId: GameId, hostColor: chess.Color) =
    updatePairing(gameId, _.copy(hostColor = hostColor))

  private def Created(s: => Simul): Simul = if (isCreated) s else this

  def wins    = pairings.count(p => p.finished && p.wins.has(false))
  def draws   = pairings.count(p => p.finished && p.wins.isEmpty)
  def losses  = pairings.count(p => p.finished && p.wins.has(true))
  def ongoing = pairings.count(_.ongoing)

  def pairingOf(userId: UserId) = pairings.find(_ is userId)

object Simul:

  case class OnStart(simul: Simul) extends AnyVal

  def make(
      host: User,
      name: String,
      clock: SimulClock,
      variants: List[Variant],
      position: Option[Fen.Epd],
      color: String,
      text: String,
      estimatedStartAt: Option[Instant],
      team: Option[TeamId],
      featurable: Option[Boolean]
  ): Simul = Simul(
    _id = SimulId(ThreadLocalRandom nextString 8),
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
    createdAt = nowInstant,
    estimatedStartAt = estimatedStartAt,
    variants = if (position.isDefined) List(chess.variant.Standard) else variants,
    position = position,
    applicants = Nil,
    pairings = Nil,
    startedAt = none,
    finishedAt = none,
    hostSeenAt = nowInstant.some,
    color = color.some,
    text = text,
    team = team,
    featurable = featurable
  )
