package lila.tournament

import chess.Clock.Config as ClockConfig
import chess.format.Fen
import chess.{ Mode, Speed }
import play.api.i18n.Lang
import scala.util.chaining.*
import ornicar.scalalib.ThreadLocalRandom

import lila.common.GreatPlayer
import lila.i18n.defaultLang
import lila.rating.PerfType
import lila.user.User

case class Tournament(
    id: TourId,
    name: String,
    status: Status,
    clock: ClockConfig,
    minutes: Int,
    variant: chess.variant.Variant,
    position: Option[Fen.Opening],
    mode: Mode,
    password: Option[String] = None,
    conditions: Condition.All,
    teamBattle: Option[TeamBattle] = None,
    noBerserk: Boolean = false,
    noStreak: Boolean = false,
    schedule: Option[Schedule],
    nbPlayers: Int,
    createdAt: Instant,
    createdBy: UserId,
    startsAt: Instant,
    winnerId: Option[UserId] = None,
    featuredId: Option[GameId] = None,
    spotlight: Option[Spotlight] = None,
    description: Option[String] = None,
    hasChat: Boolean = true
):

  def isCreated   = status == Status.Created
  def isStarted   = status == Status.Started
  def isFinished  = status == Status.Finished
  def isEnterable = !isFinished

  def isPrivate = password.isDefined

  def isTeamBattle = teamBattle.isDefined

  def name(full: Boolean = true)(using Lang): String =
    if (isMarathon || isUnique) name
    else if (isTeamBattle && full) lila.i18n.I18nKeys.tourname.xTeamBattle.txt(name)
    else if (isTeamBattle) name
    else schedule.fold(if (full) s"$name Arena" else name)(_.name(full))

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

  def secondsToStart = (startsAt.toSeconds - nowSeconds).toInt atLeast 0

  def secondsToFinish = (finishesAt.toSeconds - nowSeconds).toInt atLeast 0

  def pairingsClosed = secondsToFinish < math.max(30, math.min(clock.limitSeconds.value / 2, 120))

  def isStillWorthEntering =
    isMarathonOrUnique || {
      secondsToFinish > (minutes * 60 / 3).atMost(20 * 60)
    }

  def finishedSinceSeconds: Option[Long] = isFinished option (nowSeconds - finishesAt.toSeconds)

  def isRecentlyFinished = finishedSinceSeconds.exists(_ < 30 * 60)

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.toSeconds) < 15

  def isNowOrSoon = startsAt.isBefore(nowInstant plusMinutes 15) && !isFinished

  def isDistant = startsAt.isAfter(nowInstant plusDays 1)

  def duration = java.time.Duration.ofMinutes(minutes)

  def interval = TimeInterval(startsAt, duration)

  def overlaps(other: Tournament) = interval overlaps other.interval

  def similarTo(other: Tournament) =
    (schedule, other.schedule) match
      case (Some(s1), Some(s2)) if s1 similarTo s2 => true
      case _                                       => false

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

  def startingPosition = position flatMap Thematic.byFen

  lazy val looksLikePrize = !isScheduled && lila.common.String.looksLikePrize(s"$name $description")

  def estimateNumberOfGamesOneCanPlay: Double =
    // There are 2 players, and they don't always use all their time (0.8)
    // add 15 seconds for pairing delay
    val estimatedGameSeconds: Double = clock.estimateTotalSeconds * 2 * 0.8 + 15
    (minutes * 60) / estimatedGameSeconds

  override def toString =
    s"$id $startsAt ${name()(using defaultLang)} $minutes minutes, $clock, $nbPlayers players"

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament:

  val minPlayers = 2

  def make(
      by: Either[UserId, User],
      name: Option[String],
      clock: ClockConfig,
      minutes: Int,
      variant: chess.variant.Variant,
      position: Option[Fen.Opening],
      mode: Mode,
      password: Option[String],
      waitMinutes: Int,
      startDate: Option[Instant],
      berserkable: Boolean,
      streakable: Boolean,
      teamBattle: Option[TeamBattle],
      description: Option[String],
      hasChat: Boolean
  ) =
    Tournament(
      id = makeId,
      name = name | (position match {
        case Some(pos) => Thematic.byFen(pos).fold("Custom position")(_.name.value)
        case None      => GreatPlayer.randomName
      }),
      status = Status.Created,
      clock = clock,
      minutes = minutes,
      createdBy = by.fold(identity, _.id),
      createdAt = nowInstant,
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
      startsAt = startDate | nowInstant.plusMinutes(waitMinutes),
      description = description,
      hasChat = hasChat
    )

  def scheduleAs(sched: Schedule, minutes: Int) =
    Tournament(
      id = makeId,
      name = sched.name(full = false)(using defaultLang),
      status = Status.Created,
      clock = Schedule clockFor sched,
      minutes = minutes,
      createdBy = User.lichessId,
      createdAt = nowInstant,
      nbPlayers = 0,
      variant = sched.variant,
      position = sched.position,
      mode = Mode.Rated,
      conditions = sched.conditions,
      schedule = Some(sched),
      startsAt = sched.at.instant plusSeconds ThreadLocalRandom.nextInt(60)
    )

  def tournamentUrl(tourId: TourId): String = s"https://lichess.org/tournament/$tourId"

  def makeId = TourId(ThreadLocalRandom nextString 8)

  case class PastAndNext(past: List[Tournament], next: List[Tournament])

  enum JoinResult(val error: Option[String]):
    def ok = error.isEmpty
    case Ok             extends JoinResult(none)
    case WrongEntryCode extends JoinResult("Wrong entry code".some)
    case Paused         extends JoinResult("Your pause is not over yet".some)
    case Verdicts       extends JoinResult("Tournament restrictions".some)
    case MissingTeam    extends JoinResult("Missing team".some)
    case Nope           extends JoinResult("Couldn't join for some reason?".some)
