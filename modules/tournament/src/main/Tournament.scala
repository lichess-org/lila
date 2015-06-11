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
    createdAt: DateTime,
    createdBy: String,
    startsAt: DateTime) {

  def isCreated = status == Status.Created
  def isStarted = status == Status.Started
  def isFinished = status == Status.Finished

  def fullName = s"$name $system"

  def scheduled = schedule.isDefined

  def finishesAt = startsAt plusMinutes minutes

  def hasWaitedEnough = startsAt isBefore DateTime.now

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt max 0

  def secondsToFinish = (finishesAt.getSeconds - nowSeconds).toInt max 0

  def isAlmostFinished = secondsToFinish < math.max(30, math.min(clock.limit / 2, 120))

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

// def userCurrentPairing(userId: String): Option[Pairing] = pairings find { p =>
//   p.playing && p.contains(userId)
// }

// def userCurrentPov(userId: String): Option[PovRef] =
//   userCurrentPairing(userId) flatMap (_ povRef userId)

// def userCurrentPov(user: Option[User]): Option[PovRef] =
//   user.flatMap(u => userCurrentPov(u.id))

// def finish = withPlayers(players.map(_.unWithdraw)).refreshPlayers |> { tour =>
//   Finished(
//     id = tour.id,
//     data = tour.data,
//     startedAt = tour.startedAt,
//     players = tour.players.map(_.unWithdraw),
//     // pairings = tour.pairings filterNot (_.playing),
//     events = tour.events)
// }

// def withdraw(userId: String): Valid[Started] = contains(userId).fold(
//   withPlayers(players map {
//     case p if p is userId => p.doWithdraw
//     case p                => p
//   }).success,
//   !!("User %s is not part of the tournament" format userId)
// )

// def quickLossStreak(user: String): Boolean =
//   userPairings(user).takeWhile { pair => (pair lostBy user) && pair.quickFinish }.size >= 3

// def withPlayers(s: Players) = copy(players = s)
// def refreshPlayers = withPlayers(Player refresh this)

// def join(user: User) = joinNew(user) orElse joinBack(user)

// private def joinBack(user: User) = withdrawnPlayers.find(_ is user) match {
//   case None => !!("User %s is already part of the tournament" format user.id)
//   case Some(player) => withPlayers(players map {
//     case p if p is player => p.unWithdraw
//     case p                => p
//   }).success
// }
// }

// case class Finished(
//     id: String,
//     data: Data,
//     startedAt: DateTime,
//     events: List[Event]) extends StartedOrFinished {

//   override def isFinished = true

//   // def withPlayers(s: Players) = copy(players = s)
//   // def refreshPlayers = withPlayers(Player refresh this)
// }

object Tournament {

  val minPlayers = 3

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
    variant = sched.variant,
    position = StartingPosition.initial,
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
