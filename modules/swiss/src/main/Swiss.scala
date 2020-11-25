package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.Speed
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.hub.LightTeam.TeamID
import lila.rating.PerfType
import lila.user.User
import chess.format.FEN

case class Swiss(
    _id: Swiss.Id,
    name: String,
    clock: ClockConfig,
    variant: chess.variant.Variant,
    round: SwissRound.Number, // ongoing round
    nbPlayers: Int,
    nbOngoing: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    teamId: TeamID,
    startsAt: DateTime,
    settings: Swiss.Settings,
    nextRoundAt: Option[DateTime],
    finishedAt: Option[DateTime],
    winnerId: Option[User.ID] = None
) {
  def id = _id

  def isCreated          = round.value == 0
  def isStarted          = !isCreated && !isFinished
  def isFinished         = finishedAt.isDefined
  def isNotFinished      = !isFinished
  def isNowOrSoon        = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished
  def isRecentlyFinished = finishedAt.exists(f => (nowSeconds - f.getSeconds) < 30 * 60)
  def isEnterable =
    isNotFinished && round.value <= settings.nbRounds / 2 && nbPlayers < Swiss.maxPlayers

  def allRounds: List[SwissRound.Number]      = (1 to round.value).toList.map(SwissRound.Number.apply)
  def finishedRounds: List[SwissRound.Number] = (1 until round.value).toList.map(SwissRound.Number.apply)

  def guessNbRounds  = (nbPlayers - 1) atMost settings.nbRounds atLeast 2
  def actualNbRounds = if (isFinished) round.value else guessNbRounds

  def startRound =
    copy(
      round = SwissRound.Number(round.value + 1),
      nextRoundAt = none
    )

  def speed = Speed(clock)

  def perfType: PerfType = PerfType(variant, speed)

  def estimatedDuration: FiniteDuration = {
    (clock.limit.toSeconds + clock.increment.toSeconds * 80 + 10) * settings.nbRounds
  }.toInt.seconds

  def estimatedDurationString = {
    val minutes = estimatedDuration.toMinutes
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${minutes % 60}m" else "")
  }

  def roundInfo = Swiss.RoundInfo(teamId, settings.chatFor)

  def withConditions(conditions: SwissCondition.All) = copy(
    settings = settings.copy(conditions = conditions)
  )

  lazy val looksLikePrize = lila.common.String.looksLikePrize(s"$name ${~settings.description}")
}

object Swiss {

  val maxPlayers = 4000

  case class Id(value: String) extends AnyVal with StringValue
  case class Round(value: Int) extends AnyVal with IntValue

  case class Points(double: Int) extends AnyVal {
    def value: Float = double / 2f
    def +(p: Points) = Points(double + p.double)
  }
  case class TieBreak(value: Double)   extends AnyVal
  case class Performance(value: Float) extends AnyVal
  case class Score(value: Int)         extends AnyVal

  case class Settings(
      nbRounds: Int,
      rated: Boolean,
      description: Option[String] = None,
      position: Option[FEN],
      chatFor: ChatFor = ChatFor.default,
      password: Option[String] = None,
      conditions: SwissCondition.All,
      roundInterval: FiniteDuration
  ) {
    lazy val intervalSeconds = roundInterval.toSeconds.toInt
    def manualRounds         = intervalSeconds == Swiss.RoundInterval.manual
    def dailyInterval        = (!manualRounds && intervalSeconds >= 24 * 3600) option intervalSeconds / 3600 / 24
  }

  type ChatFor = Int
  object ChatFor {
    val NONE    = 0
    val LEADERS = 10
    val MEMBERS = 20
    val ALL     = 30
    val default = MEMBERS
  }

  object RoundInterval {
    val auto   = -1
    val manual = 99999999
  }

  def makeScore(points: Points, tieBreak: TieBreak, perf: Performance) =
    Score(
      (points.value * 10000000 + tieBreak.value * 10000 + perf.value).toInt
    )

  def makeId = Id(lila.common.ThreadLocalRandom nextString 8)

  case class PastAndNext(past: List[Swiss], next: List[Swiss])

  case class RoundInfo(
      teamId: TeamID,
      chatFor: ChatFor
  )
}
