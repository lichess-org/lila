package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.Speed
import org.joda.time.DateTime
import scala.concurrent.duration._

import lila.game.PerfPicker
import lila.hub.LightTeam.TeamID
import lila.rating.PerfType
import lila.user.User

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
  def isEnterable        = isNotFinished && round.value <= settings.nbRounds / 2
  def isRecentlyFinished = finishedAt.exists(f => (nowSeconds - f.getSeconds) < 30 * 60)

  def allRounds: List[SwissRound.Number]      = (1 to round.value).toList.map(SwissRound.Number.apply)
  def finishedRounds: List[SwissRound.Number] = (1 to (round.value - 1)).toList.map(SwissRound.Number.apply)

  def guessNbRounds  = (nbPlayers - 1) atMost settings.nbRounds
  def actualNbRounds = if (isFinished) round.value else guessNbRounds

  def startRound =
    copy(
      round = SwissRound.Number(round.value + 1),
      nextRoundAt = none
    )

  def speed = Speed(clock)

  def perfType: Option[PerfType] = PerfPicker.perfType(speed, variant, none)
  def perfLens                   = PerfPicker.mainOrDefault(speed, variant, none)

  def estimatedDuration: FiniteDuration = {
    (clock.limit.toSeconds + clock.increment.toSeconds * 80 + 10) * settings.nbRounds
  }.toInt.seconds

  def estimatedDurationString = {
    val minutes = estimatedDuration.toMinutes
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")
  }
}

object Swiss {

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
      hasChat: Boolean = true,
      roundInterval: FiniteDuration
  )

  def makeScore(points: Points, tieBreak: TieBreak, perf: Performance) =
    Score(
      (points.value * 10000000 + tieBreak.value * 10000 + perf.value).toInt
    )

  def makeId = Id(scala.util.Random.alphanumeric take 8 mkString)
}
