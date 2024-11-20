package lila.simul

import chess.format.Fen
import chess.variant.Variant
import chess.{ Color, Speed }
import chess.IntRating
import chess.rating.RatingProvisional
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.perf.UserWithPerfs
import lila.core.rating.Score
import lila.rating.PerfType
import lila.rating.UserPerfsExt.bestPerf

case class Simul(
    @Key("_id") id: SimulId,
    name: String,
    status: SimulStatus,
    clock: SimulClock,
    applicants: List[SimulApplicant],
    pairings: List[SimulPairing],
    variants: List[Variant],
    position: Option[Fen.Full],
    createdAt: Instant,
    estimatedStartAt: Option[Instant] = None,
    hostId: UserId,
    hostRating: IntRating,
    hostProvisional: Option[RatingProvisional],
    hostGameId: Option[String], // game the host is focusing on
    startedAt: Option[Instant],
    finishedAt: Option[Instant],
    hostSeenAt: Option[Instant],
    color: Option[String],
    text: String,
    featurable: Option[Boolean],
    conditions: SimulCondition.All
) extends lila.core.simul.Simul:
  def fullName = s"$name simul"

  def isCreated = !isStarted

  def isStarted = startedAt.isDefined

  def isFinished = status == SimulStatus.Finished

  def isRunning = status == SimulStatus.Started

  def hasApplicant(userId: UserId) = applicants.exists(_.is(userId))

  def hasPairing(userId: UserId) = pairings.exists(_.is(userId))

  def hasUser(userId: UserId) = hasApplicant(userId) || hasPairing(userId)

  def addApplicant(applicant: SimulApplicant) =
    Created:
      if !hasApplicant(applicant.player.user) && variants.has(applicant.player.variant)
      then copy(applicants = applicants :+ applicant)
      else this

  def removeApplicant(userId: UserId) =
    Created:
      copy(applicants = applicants.filterNot(_.is(userId)))

  def accept(userId: UserId, v: Boolean) =
    Created:
      copy(applicants = applicants.map: a =>
        if a.is(userId) then a.copy(accepted = v) else a)

  def removePairing(userId: UserId) =
    copy(pairings = pairings.filterNot(_.is(userId))).finishIfDone

  def nbAccepted = applicants.count(_.accepted)

  def startable = isCreated && nbAccepted > 1

  def start =
    startable.option(
      copy(
        status = SimulStatus.Started,
        startedAt = nowInstant.some,
        applicants = Nil,
        clock = clock.adjustedForPlayers(nbAccepted),
        pairings = applicants.collect {
          case a if a.accepted => SimulPairing(a.player)
        },
        hostSeenAt = none
      )
    )

  def updatePairing(gameId: GameId, f: SimulPairing => SimulPairing) =
    copy(
      pairings = pairings.map: p =>
        if p.gameId == gameId then f(p) else p
    ).finishIfDone

  def ejectCheater(userId: UserId): Option[Simul] =
    hasUser(userId).option(removeApplicant(userId).removePairing(userId))

  private def finishIfDone =
    if isStarted && pairings.forall(_.finished) then
      copy(
        status = SimulStatus.Finished,
        finishedAt = nowInstant.some,
        hostGameId = none
      )
    else this

  def gameIds        = pairings.map(_.gameId)
  def ongoingGameIds = pairings.filter(_.ongoing).map(_.gameId)

  def perfTypes: List[PerfType] =
    variants.map:
      lila.rating.PerfType(_, Speed(clock.config.some))

  def mainPerfType =
    perfTypes
      .find(pt => lila.rating.PerfType.variantOf(pt).standard)
      .orElse(perfTypes.headOption)
      .getOrElse(PerfType.Rapid)

  def applicantRatio = s"${applicants.count(_.accepted)}/${applicants.size}"

  def variantRich = variants.sizeIs > 3

  def isHost(userOption: Option[User]): Boolean = userOption.so(isHost)
  def isHost(user: User): Boolean               = user.id == hostId

  def playingPairings = pairings.filterNot(_.finished)

  def hostColor: Option[Color] = color.flatMap(Color.fromName)

  def setPairingHostColor(gameId: GameId, hostColor: Color) =
    updatePairing(gameId, _.copy(hostColor = hostColor))

  private def Created(s: => Simul): Simul = if isCreated then s else this

  def wins    = pairings.count(p => p.finished && p.wins.has(false))
  def draws   = pairings.count(p => p.finished && p.wins.isEmpty)
  def losses  = pairings.count(p => p.finished && p.wins.has(true))
  def ongoing = pairings.count(_.ongoing)

  def pairingOf(userId: UserId) = pairings.find(_.is(userId))
  def playerIds                 = pairings.map(_.player.user)
  def hostScore                 = lila.core.rating.Score(wins, losses, draws, none)
  def playerScore(userId: UserId) = pairingOf(userId).map: p =>
    Score(p.wins.has(true).so(1), p.wins.has(false).so(1), p.wins.isEmpty.so(1), none)

object Simul:

  def make(
      host: UserWithPerfs,
      name: String,
      clock: SimulClock,
      variants: List[Variant],
      position: Option[Fen.Full],
      color: String,
      text: String,
      estimatedStartAt: Option[Instant],
      featurable: Option[Boolean],
      conditions: SimulCondition.All
  ): Simul =
    val hostPerf = host.perfs.bestPerf(variants.map { lila.rating.PerfType(_, Speed(clock.config.some)) })
    Simul(
      id = SimulId(ThreadLocalRandom.nextString(8)),
      name = name,
      status = SimulStatus.Created,
      clock = clock,
      hostId = host.id,
      hostRating = hostPerf.intRating,
      hostProvisional = hostPerf.provisional.some,
      hostGameId = none,
      createdAt = nowInstant,
      estimatedStartAt = estimatedStartAt,
      variants = if position.isDefined then List(chess.variant.Standard) else variants,
      position = position,
      applicants = Nil,
      pairings = Nil,
      startedAt = none,
      finishedAt = none,
      hostSeenAt = nowInstant.some,
      color = color.some,
      text = text,
      featurable = featurable,
      conditions = conditions
    )
