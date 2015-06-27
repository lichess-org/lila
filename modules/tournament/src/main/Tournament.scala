package lila.tournament

import org.joda.time.{ DateTime, Duration, Interval }
import ornicar.scalalib.Random

import chess.{ Speed, Mode, StartingPosition }
import lila.game.{ PovRef, PerfPicker }
import lila.user.User

case class Tournament(
    id: String,
    name: String,
    status: Status,
    system: System,
    clock: TournamentClock,
    minutes: Int,
    variant: chess.variant.Variant,
    position: StartingPosition,
    mode: Mode,
    `private`: Boolean,
    schedule: Option[Schedule],
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: String,
    startsAt: DateTime,
    winnerId: Option[String] = None) {

  def isCreated = status == Status.Created
  def isStarted = status == Status.Started
  def isFinished = status == Status.Finished

  def fullName = schedule.map(_.freq) match {
    case Some(Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon) => name
    case _ => s"$name $system"
  }

  def scheduled = schedule.isDefined

  def finishesAt = startsAt plusMinutes minutes

  def hasWaitedEnough = startsAt isBefore DateTime.now

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt max 0

  def secondsToFinish = (finishesAt.getSeconds - nowSeconds).toInt max 0

  def isAlmostFinished = secondsToFinish < math.max(30, math.min(clock.limit / 2, 120))

  def isStillWorthEntering = secondsToFinish > minutes * 60 / 2

  def isRecentlyFinished = isFinished && (nowSeconds - finishesAt.getSeconds) < 30 * 60

  def duration = new Duration(minutes * 60 * 1000)

  def interval = new Interval(startsAt, finishesAt)

  def overlaps(other: Tournament) = interval overlaps other.interval

  def speed = Speed(clock.chessClock.some)

  def perfType = PerfPicker.perfType(speed, variant, none)
  def perfLens = PerfPicker.mainOrDefault(speed, variant, none)

  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")

  def berserkable = system.berserkable && clock.increment == 0

  def clockStatus = secondsToFinish |> { s => "%02d:%02d".format(s / 60, s % 60) }
}

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament {

  val minPlayers = 2

  def make(
    createdBy: User,
    clock: TournamentClock,
    minutes: Int,
    system: System,
    variant: chess.variant.Variant,
    position: StartingPosition,
    mode: Mode,
    `private`: Boolean,
    waitMinutes: Int) = Tournament(
    id = Random nextStringUppercase 8,
    name = if (position.initial) RandomName() else position.shortName,
    status = Status.Created,
    system = system,
    clock = clock,
    minutes = minutes,
    createdBy = createdBy.id,
    createdAt = DateTime.now,
    nbPlayers = 0,
    variant = variant,
    position = position,
    mode = mode,
    `private` = `private`,
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
    schedule = Some(sched),
    startsAt = sched.at)

  // To sort combined sequences of pairings and events.
  // This sorts all pairings/events in chronological order. Pairings without a timestamp
  // are assumed to have happened at the origin of time (i.e. before anything else).
  // object PairingEventOrdering extends Ordering[Either[Pairing, Event]] {
  //   def compare(x: Either[Pairing, Event], y: Either[Pairing, Event]): Int = {
  //     val ot1: Option[DateTime] = x.fold(_.date.some, e => Some(e.timestamp))
  //     val ot2: Option[DateTime] = y.fold(_.date.some, e => Some(e.timestamp))

  //     (ot1, ot2) match {
  //       case (None, None)         => 0
  //       case (None, Some(_))      => -1
  //       case (Some(_), None)      => 1
  //       case (Some(t1), Some(t2)) => if (t1 equals t2) 0 else if (t1 isBefore t2) -1 else 1
  //     }
  //   }
  // }
}
