package lidraughts.tournament

import org.joda.time.{ DateTime, Duration, Interval }
import ornicar.scalalib.Random

import draughts.Clock.{ Config => ClockConfig }
import draughts.{ Speed, Mode, StartingPosition, OpeningTable }
import lidraughts.game.PerfPicker
import lidraughts.rating.PerfType
import lidraughts.user.User

case class Tournament(
    id: Tournament.ID,
    name: String,
    status: Status,
    system: System,
    clock: ClockConfig,
    minutes: Int,
    variant: draughts.variant.Variant,
    position: StartingPosition,
    openingTable: Option[OpeningTable],
    mode: Mode,
    password: Option[String] = None,
    conditions: Condition.All,
    noBerserk: Boolean = false,
    schedule: Option[Schedule],
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    startsAt: DateTime,
    winnerId: Option[User.ID] = None,
    featuredId: Option[String] = None,
    spotlight: Option[Spotlight] = None,
    description: Option[String] = None
) {

  def isCreated = status == Status.Created
  def isStarted = status == Status.Started
  def isFinished = status == Status.Finished

  def isPrivate = password.isDefined
  def isHidden = isPrivate && !isUnique

  def fullName = schedule.map(_.freq).fold(s"$name $system") {
    case Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon | Schedule.Freq.Unique => name
    case Schedule.Freq.Shield => s"$name $system"
    case _ if clock.hasIncrement => s"$name Inc $system"
    case _ => s"$name $system"
  }

  def isMarathon = schedule.map(_.freq) exists {
    case Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon => true
    case _ => false
  }

  def isThematic = openingTable.isDefined || !position.initialVariant(variant)

  def isShield = schedule.map(_.freq) has Schedule.Freq.Shield

  def isUnique = schedule.map(_.freq) has Schedule.Freq.Unique

  def isMarathonOrUnique = isMarathon || isUnique

  def isScheduled = schedule.isDefined

  def finishesAt = startsAt plusMinutes minutes

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt atLeast 0

  def secondsToFinish = (finishesAt.getSeconds - nowSeconds).toInt atLeast 0

  def pairingsClosed = secondsToFinish < math.max(30, math.min(clock.limitSeconds / 2, 120))

  def isStillWorthEntering = isMarathonOrUnique || {
    secondsToFinish > (minutes * 60 / 3).atMost(20 * 60)
  }

  def isRecentlyFinished = isFinished && (nowSeconds - finishesAt.getSeconds) < 30 * 60

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.getSeconds) < 15

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished

  def isDistant = startsAt.isAfter(DateTime.now plusDays 1)

  def duration = new Duration(minutes * 60 * 1000)

  def interval = new Interval(startsAt, finishesAt)

  def overlaps(other: Tournament) = interval overlaps other.interval

  def similarTo(other: Tournament) = (schedule, other.schedule) match {
    case (Some(s1), Some(s2)) if s1 similarTo s2 => true
    case _ => false
  }

  def speed = Speed(clock)

  def perfType: Option[PerfType] = PerfPicker.perfType(speed, variant, none)
  def perfLens = PerfPicker.mainOrDefault(speed, variant, none)

  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")

  def berserkable = !noBerserk && system.berserkable && clock.berserkable

  def clockStatus = secondsToFinish |> { s => "%02d:%02d".format(s / 60, s % 60) }

  def schedulePair = schedule map { this -> _ }

  def winner = winnerId map { userId =>
    Winner(
      tourId = id,
      userId = userId,
      tourName = name,
      date = finishesAt
    )
  }

  def nonLidraughtsCreatedBy = (createdBy != User.lidraughtsId) option createdBy

  def ratingVariant = if (variant.fromPosition) draughts.variant.Standard else variant

  override def toString = s"$id $startsAt $fullName $minutes minutes, $clock"
}

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament {

  type ID = String

  def make(
    by: Either[User.ID, User],
    name: Option[String],
    clock: ClockConfig,
    minutes: Int,
    system: System,
    variant: draughts.variant.Variant,
    position: StartingPosition,
    openingTable: Option[OpeningTable],
    mode: Mode,
    password: Option[String],
    waitMinutes: Int,
    startDate: Option[DateTime],
    berserkable: Boolean,
    description: Option[String]
  ) = Tournament(
    id = makeId,
    name = name | {
      if (position.name.isEmpty || position.initialVariant(variant)) GreatPlayer.randomName
      else position.shortName
    },
    status = Status.Created,
    system = system,
    clock = clock,
    minutes = minutes,
    createdBy = by.fold(identity, _.id),
    createdAt = DateTime.now,
    nbPlayers = 0,
    variant = variant,
    position = position,
    openingTable = openingTable,
    mode = mode,
    password = password,
    conditions = Condition.All.empty,
    noBerserk = !berserkable,
    schedule = None,
    startsAt = startDate | {
      DateTime.now plusMinutes waitMinutes
    },
    description = description
  )

  def schedule(sched: Schedule, minutes: Int) = Tournament(
    id = makeId,
    name = sched.name,
    status = Status.Created,
    system = System.default,
    clock = Schedule clockFor sched,
    minutes = minutes,
    createdBy = User.lidraughtsId,
    createdAt = DateTime.now,
    nbPlayers = 0,
    variant = sched.variant,
    position = sched.position,
    openingTable = none,
    mode = Mode.Rated,
    conditions = sched.conditions,
    schedule = Some(sched),
    startsAt = sched.at
  )

  def makeId = Random nextString 8
}
