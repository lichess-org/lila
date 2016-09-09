package lila.tournament

import org.joda.time.{ DateTime, Duration, Interval }
import ornicar.scalalib.Random

import chess.{ Speed, Mode, StartingPosition }
import lila.game.{ PovRef, PerfPicker }
import lila.user.User

case class Tournament(
    id: Tournament.ID,
    name: String,
    status: Status,
    system: System,
    clock: TournamentClock,
    minutes: Int,
    variant: chess.variant.Variant,
    position: StartingPosition,
    mode: Mode,
    `private`: Boolean,
    password: Option[String] = None,
    conditions: Condition.All,
    schedule: Option[Schedule],
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: String,
    startsAt: DateTime,
    winnerId: Option[String] = None,
    featuredId: Option[String] = None,
    spotlight: Option[Spotlight] = None) {

  def isCreated = status == Status.Created
  def isStarted = status == Status.Started
  def isFinished = status == Status.Finished

  def isPrivate = `private`

  def fullName =
    if (isMarathonOrUnique) name
    else if (isScheduled && clock.hasIncrement) s"$name Inc $system"
    else s"$name $system"

  def isMarathon = schedule.map(_.freq) exists {
    case Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon => true
    case _ => false
  }

  def isUnique = schedule.map(_.freq) contains Schedule.Freq.Unique

  def isMarathonOrUnique = isMarathon || isUnique

  def isScheduled = schedule.isDefined

  def finishesAt = startsAt plusMinutes minutes

  def hasWaitedEnough = startsAt isBefore DateTime.now

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt max 0

  def secondsToFinish = (finishesAt.getSeconds - nowSeconds).toInt max 0

  def pairingsClosed = secondsToFinish < math.max(30, math.min(clock.limit / 2, 120))

  def isStillWorthEntering = isMarathonOrUnique || {
    secondsToFinish > (minutes * 60 / 3).atMost(20 * 60)
  }

  def isRecentlyFinished = isFinished && (nowSeconds - finishesAt.getSeconds) < 30 * 60

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.getSeconds) < 15

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished

  def duration = new Duration(minutes * 60 * 1000)

  def interval = new Interval(startsAt, finishesAt)

  def overlaps(other: Tournament) = interval overlaps other.interval

  def similarTo(other: Tournament) = (schedule, other.schedule) match {
    case (Some(s1), Some(s2)) if s1 similarTo s2 => true
    case _                                       => false
  }

  def speed = Speed(clock.chessClock.some)

  def perfType = PerfPicker.perfType(speed, variant, none)
  def perfLens = PerfPicker.mainOrDefault(speed, variant, none)

  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")

  def berserkable = system.berserkable && clock.chessClock.berserkable

  def clockStatus = secondsToFinish |> { s => "%02d:%02d".format(s / 60, s % 60) }

  def schedulePair = schedule map { this -> _ }

  override def toString = s"$id $startsAt $fullName $minutes minutes, $clock"
}

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament {

  type ID = String

  val minPlayers = 2

  def make(
    createdByUserId: String,
    clock: TournamentClock,
    minutes: Int,
    system: System,
    variant: chess.variant.Variant,
    position: StartingPosition,
    mode: Mode,
    `private`: Boolean,
    password: Option[String],
    waitMinutes: Int) = Tournament(
    id = Random nextStringUppercase 8,
    name = if (position.initial) GreatPlayer.randomName else position.shortName,
    status = Status.Created,
    system = system,
    clock = clock,
    minutes = minutes,
    createdBy = createdByUserId,
    createdAt = DateTime.now,
    nbPlayers = 0,
    variant = variant,
    position = position,
    mode = mode,
    `private` = `private`,
    password = password,
    conditions = Condition.All.empty,
    schedule = None,
    startsAt = DateTime.now plusMinutes waitMinutes)

  def schedule(sched: Schedule, minutes: Int) = Tournament(
    id = Random nextStringUppercase 8,
    name = sched.name,
    status = Status.Created,
    system = System.default,
    clock = Schedule clockFor sched,
    minutes = minutes,
    createdBy = "lichess",
    createdAt = DateTime.now,
    nbPlayers = 0,
    variant = sched.variant,
    position = sched.position,
    mode = Mode.Rated,
    `private` = false,
    conditions = sched.conditions,
    schedule = Some(sched),
    startsAt = sched.at)
}
