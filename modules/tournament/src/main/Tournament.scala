package lila.tournament

import chess.Clock.{ Config => ClockConfig }
import chess.format.FEN
import chess.{ Mode, Speed, StartingPosition }
import org.joda.time.{ DateTime, Duration, Interval }
import play.api.i18n.Lang
import scala.util.chaining._

import lila.common.GreatPlayer
import lila.common.ThreadLocalRandom
import lila.i18n.defaultLang
import lila.rating.PerfType
import lila.user.User

case class Tournament(
    id: Tournament.ID,
    name: String,
    status: Status,
    clock: ClockConfig,
    minutes: Int,
    variant: chess.variant.Variant,
    position: Either[StartingPosition, FEN],
    mode: Mode,
    password: Option[String] = None,
    conditions: Condition.All,
    teamBattle: Option[TeamBattle] = None,
    noBerserk: Boolean = false,
    noStreak: Boolean = false,
    schedule: Option[Schedule],
    nbPlayers: Int,
    createdAt: DateTime,
    createdBy: User.ID,
    startsAt: DateTime,
    winnerId: Option[User.ID] = None,
    featuredId: Option[String] = None,
    spotlight: Option[Spotlight] = None,
    description: Option[String] = None,
    hasChat: Boolean = true
) {

  def isCreated   = status == Status.Created
  def isStarted   = status == Status.Started
  def isFinished  = status == Status.Finished
  def isEnterable = !isFinished

  def isPrivate = password.isDefined

  def isTeamBattle = teamBattle.isDefined

  def name(full: Boolean = true)(implicit lang: Lang): String = {
    import lila.i18n.I18nKeys.tourname._
    if (isMarathon || isUnique) name
    else if (isTeamBattle && full) xTeamBattle.txt(name)
    else if (isTeamBattle) name
    else schedule.fold(if (full) s"$name Arena" else name)(_.name(full))
  }

  def isMarathon =
    schedule.map(_.freq) exists {
      case Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon => true
      case _                                                           => false
    }

  def isShield = schedule.map(_.freq) has Schedule.Freq.Shield

  def isUnique = schedule.map(_.freq) has Schedule.Freq.Unique

  def isMarathonOrUnique = isMarathon || isUnique

  def isScheduled = schedule.isDefined

  def isRated = mode == Mode.Rated

  def finishesAt = startsAt plusMinutes minutes

  def secondsToStart = (startsAt.getSeconds - nowSeconds).toInt atLeast 0

  def secondsToFinish = (finishesAt.getSeconds - nowSeconds).toInt atLeast 0

  def pairingsClosed = secondsToFinish < math.max(30, math.min(clock.limitSeconds / 2, 120))

  def isStillWorthEntering =
    isMarathonOrUnique || {
      secondsToFinish > (minutes * 60 / 3).atMost(20 * 60)
    }

  def isRecentlyFinished = isFinished && (nowSeconds - finishesAt.getSeconds) < 30 * 60

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.getSeconds) < 15

  def isNowOrSoon = startsAt.isBefore(DateTime.now plusMinutes 15) && !isFinished

  def isDistant = startsAt.isAfter(DateTime.now plusDays 1)

  def duration = new Duration(minutes * 60 * 1000)

  def interval = new Interval(startsAt, duration)

  def overlaps(other: Tournament) = interval overlaps other.interval

  def similarTo(other: Tournament) =
    (schedule, other.schedule) match {
      case (Some(s1), Some(s2)) if s1 similarTo s2 => true
      case _                                       => false
    }

  def speed = Speed(clock)

  def perfType: PerfType = PerfType(variant, speed)

  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${minutes % 60}m" else "")

  def berserkable = !noBerserk && clock.berserkable
  def streakable  = !noStreak

  def clockStatus =
    secondsToFinish pipe { s =>
      "%02d:%02d".format(s / 60, s % 60)
    }

  def schedulePair = schedule map { this -> _ }

  def winner =
    winnerId map { userId =>
      Winner(
        tourId = id,
        userId = userId,
        tourName = name,
        date = finishesAt
      )
    }

  def nonLichessCreatedBy = (createdBy != User.lichessId) option createdBy

  def ratingVariant = if (variant.fromPosition) chess.variant.Standard else variant

  def initialPosition = position.left.exists(_.initial)

  lazy val looksLikePrize = !isScheduled && lila.common.String.looksLikePrize(s"$name $description")

  override def toString = s"$id $startsAt ${name()(defaultLang)} $minutes minutes, $clock, $nbPlayers players"
}

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament {

  type ID = String

  val minPlayers = 2

  def make(
      by: Either[User.ID, User],
      name: Option[String],
      clock: ClockConfig,
      minutes: Int,
      variant: chess.variant.Variant,
      position: Either[StartingPosition, FEN],
      mode: Mode,
      password: Option[String],
      waitMinutes: Int,
      startDate: Option[DateTime],
      berserkable: Boolean,
      streakable: Boolean,
      teamBattle: Option[TeamBattle],
      description: Option[String],
      hasChat: Boolean
  ) =
    Tournament(
      id = makeId,
      name = name | (position match {
        case Left(pos) if pos.initial => GreatPlayer.randomName
        case Left(pos)                => pos.shortName
        case _                        => GreatPlayer.randomName
      }),
      status = Status.Created,
      clock = clock,
      minutes = minutes,
      createdBy = by.fold(identity, _.id),
      createdAt = DateTime.now,
      nbPlayers = 0,
      variant = variant,
      position = position,
      mode = mode,
      password = password,
      conditions = Condition.All.empty,
      teamBattle = teamBattle,
      noBerserk = !berserkable,
      noStreak = !streakable,
      schedule = None,
      startsAt = startDate match {
        case Some(startDate) => startDate plusSeconds ThreadLocalRandom.nextInt(60)
        case None            => DateTime.now plusMinutes waitMinutes
      },
      description = description,
      hasChat = hasChat
    )

  def scheduleAs(sched: Schedule, minutes: Int) =
    Tournament(
      id = makeId,
      name = sched.name(full = false)(defaultLang),
      status = Status.Created,
      clock = Schedule clockFor sched,
      minutes = minutes,
      createdBy = User.lichessId,
      createdAt = DateTime.now,
      nbPlayers = 0,
      variant = sched.variant,
      position = Left(sched.position),
      mode = Mode.Rated,
      conditions = sched.conditions,
      schedule = Some(sched),
      startsAt = sched.at plusSeconds ThreadLocalRandom.nextInt(60)
    )

  def tournamentUrl(tourId: String): String = s"https://lichess.org/tournament/$tourId"

  def makeId = ThreadLocalRandom nextString 8

  case class PastAndNext(past: List[Tournament], next: List[Tournament])
}
