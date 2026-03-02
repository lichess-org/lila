package lila.swiss

import chess.Clock.Config as ClockConfig
import chess.format.Fen
import chess.Rated
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom

import lila.core.swiss.IdName
import lila.rating.PerfType

case class Swiss(
    @Key("_id") id: SwissId,
    name: String,
    clock: ClockConfig,
    variant: chess.variant.Variant,
    round: SwissRoundNumber, // ongoing round
    nbPlayers: Int,
    nbOngoing: Int,
    createdAt: Instant,
    createdBy: UserId,
    teamId: TeamId,
    startsAt: Instant,
    settings: Swiss.Settings,
    nextRoundAt: Option[Instant],
    finishedAt: Option[Instant],
    winnerId: Option[UserId] = None
):
  def status: Swiss.Status =
    if round.value == 0 then Swiss.Status.created
    else if finishedAt.isDefined then Swiss.Status.finished
    else Swiss.Status.started
  def isCreated = status == Swiss.Status.created
  def isStarted = status == Swiss.Status.started
  def isFinished = status == Swiss.Status.finished
  def isNotFinished = !isFinished
  def isNowOrSoon = startsAt.isBefore(nowInstant.plusMinutes(15)) && !isFinished
  def finishedSinceSeconds = finishedAt.map(nowSeconds - _.toSeconds)
  def isRecentlyFinished = finishedSinceSeconds.exists(_ < 30 * 60)
  def isEnterable =
    isNotFinished && round.value <= settings.nbRounds / 2 && nbPlayers < Swiss.maxPlayers

  def allRounds: List[SwissRoundNumber] = SwissRoundNumber.from((1 to round.value).toList)

  def startRound =
    copy(
      round = SwissRoundNumber(round.value + 1),
      nextRoundAt = none
    )

  def speed = chess.Speed(clock)

  def perfType: PerfType = lila.rating.PerfType(variant, speed)

  def estimatedDuration: FiniteDuration = {
    (clock.limit.toSeconds + clock.increment.toSeconds * 80 + 10) * settings.nbRounds
  }.toInt.seconds

  def estimatedDurationString =
    val minutes = estimatedDuration.toMinutes
    if minutes < 60 then s"${minutes}m"
    else s"${minutes / 60}h" + (if minutes % 60 != 0 then s" ${minutes % 60}m" else "")

  def roundInfo = Swiss.RoundInfo(teamId, settings.chatFor)

  def withConditions(conditions: SwissCondition.All) = copy(
    settings = settings.copy(conditions = conditions)
  )

  def unrealisticSettings =
    !settings.manualRounds &&
      settings.dailyInterval.isEmpty &&
      clock.estimateTotalSeconds * 2 * settings.nbRounds > 3600 * 8

  def idName = IdName(id, name)

  lazy val looksLikePrize = lila.gathering.looksLikePrize(s"$name ${~settings.description}")

object Swiss:

  val maxPlayers = 4000
  val maxForbiddenPairings = 1000
  val maxManualPairings = 10_000

  opaque type Round = Int
  object Round extends OpaqueInt[Round]

  opaque type TieBreak = Double
  object TieBreak extends OpaqueDouble[TieBreak]

  opaque type Performance = Float
  object Performance extends OpaqueFloat[Performance]

  opaque type Score = Int
  object Score extends OpaqueInt[Score]

  case class Settings(
      nbRounds: Int,
      rated: Rated,
      description: Option[String] = None,
      position: Option[Fen.Full],
      chatFor: ChatFor = ChatFor.default,
      password: Option[String] = None,
      conditions: SwissCondition.All,
      roundInterval: FiniteDuration,
      forbiddenPairings: String,
      manualPairings: String
  ):
    lazy val intervalSeconds = roundInterval.toSeconds.toInt
    def manualRounds = intervalSeconds == Swiss.RoundInterval.manual
    def dailyInterval = (!manualRounds && intervalSeconds >= 24 * 3600).option(intervalSeconds / 3600 / 24)

  type ChatFor = Int
  object ChatFor:
    val NONE = 0
    val LEADERS = 10
    val MEMBERS = 20
    val ALL = 30
    val default = MEMBERS

  object RoundInterval:
    val auto = -1
    val manual = 99999999

  def makeScore(points: SwissPoints, tieBreak: TieBreak, perf: Performance) =
    Score((points.value * 10000000 + tieBreak * 10000 + perf).toInt)

  def makeId = SwissId(ThreadLocalRandom.nextString(8))

  case class PastAndNext(past: List[Swiss], next: List[Swiss])

  case class RoundInfo(
      teamId: TeamId,
      chatFor: ChatFor
  )

  enum Status:
    case created, started, finished
  def status(str: String) = scala.util.Try(Status.valueOf(str)).toOption

  object Fields:
    val nbPlayers = "nbPlayers"
