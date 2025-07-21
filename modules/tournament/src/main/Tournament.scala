package lila.tournament

import chess.Clock.Config as ClockConfig
import chess.format.Fen
import chess.{ Rated, Speed }
import scalalib.ThreadLocalRandom
import scalalib.model.Seconds
import alleycats.Zero

import lila.core.i18n.Translate
import lila.core.tournament.Status
import lila.gathering.{ GreatPlayer, Thematic }
import lila.rating.PerfType

case class Tournament(
    id: TourId,
    name: String,
    status: Status,
    clock: ClockConfig,
    minutes: Int,
    variant: chess.variant.Variant,
    position: Option[Fen.Standard],
    rated: Rated,
    password: Option[String] = None,
    conditions: TournamentCondition.All,
    teamBattle: Option[TeamBattle] = None,
    noBerserk: Boolean = false,
    noStreak: Boolean = false,
    schedule: Option[Scheduled],
    nbPlayers: Int,
    createdAt: Instant,
    createdBy: UserId,
    startsAt: Instant,
    winnerId: Option[UserId] = None,
    featuredId: Option[GameId] = None,
    spotlight: Option[Spotlight] = None,
    description: Option[String] = None,
    hasChat: Boolean = true
) extends lila.core.tournament.Tournament:

  def isCreated   = status == Status.created
  def isStarted   = status == Status.started
  def isFinished  = status == Status.finished
  def isEnterable = !isFinished

  def isPrivate = password.isDefined

  def isTeamBattle  = teamBattle.isDefined
  def isTeamRelated = isTeamBattle || conditions.teamMember.isDefined

  def name(full: Boolean = true)(using Translate): String =
    if isMarathon || isUnique then name
    else if isTeamBattle && full then lila.core.i18n.I18nKey.tourname.xTeamBattle.txt(name)
    else if isTeamBattle then name
    else TournamentName(this, full)

  def scheduleFreq: Option[Schedule.Freq]   = schedule.map(_.freq)
  def scheduleSpeed: Option[Schedule.Speed] = schedule.isDefined.option(Schedule.Speed.fromClock(clock))
  def scheduleData: Option[(Schedule.Freq, Schedule.Speed)] = (scheduleFreq, scheduleSpeed).tupled

  def isMarathon = scheduleFreq.has(Schedule.Freq.Marathon)
  def isShield   = scheduleFreq.has(Schedule.Freq.Shield)
  def isUnique   = scheduleFreq.has(Schedule.Freq.Unique)

  def isScheduled = schedule.isDefined

  def finishesAt = startsAt.plusMinutes(minutes)

  def imminentStart = isCreated && (startsAt.toMillis - nowMillis) < 1000

  def secondsToStart = Seconds((startsAt.toSeconds - nowSeconds).toInt.atLeast(0))

  def secondsToFinish = Seconds((finishesAt.toSeconds - nowSeconds).toInt.atLeast(0))

  def progressPercent: Int =
    if isCreated then 0
    else if isFinished then 100
    else
      val total     = minutes * 60
      val remaining = secondsToFinish.value
      100 - (remaining * 100 / total)

  def pairingsClosed = secondsToFinish < math.max(30, math.min(clock.limitSeconds.value / 2, 120))

  def isStillWorthEntering = isMarathon || isUnique || {
    secondsToFinish > (minutes * 60 / 3).atMost(20 * 60)
  }

  def finishedSinceSeconds: Option[Long] = isFinished.option(nowSeconds - finishesAt.toSeconds)

  def isRecentlyFinished = finishedSinceSeconds.exists(_ < 30 * 60)

  def isRecentlyStarted = isStarted && (nowSeconds - startsAt.toSeconds) < 15

  def isNowOrSoon = startsAt.isBefore(nowInstant.plusMinutes(15)) && !isFinished

  def isDistant = startsAt.isAfter(nowInstant.plusDays(1))

  def duration = java.time.Duration.ofMinutes(minutes)

  def interval = TimeInterval(startsAt, duration)

  def overlaps(other: Tournament) = interval.overlaps(other.interval)

  def similarSchedule(other: Tournament) =
    schedule.isDefined && variant == other.variant && conditions == other.conditions &&
      scheduleFreq == other.scheduleFreq && scheduleSpeed == other.scheduleSpeed

  def sameNameAndTeam(other: Tournament) =
    name == other.name && conditions.teamMember == other.conditions.teamMember

  def speed = Speed(clock)

  def perfType: PerfType = lila.rating.PerfType(variant, speed)

  def durationString =
    if minutes < 60 then s"${minutes}m"
    else s"${minutes / 60}h" + (if minutes % 60 != 0 then s" ${minutes % 60}m" else "")

  def berserkable = !noBerserk && clock.berserkable
  def streakable  = !noStreak

  def clockStatus =
    val s = secondsToFinish.value
    "%02d:%02d".format(s / 60, s % 60)

  def winner =
    winnerId.map { userId =>
      Winner(
        tourId = id,
        userId = userId,
        tourName = name,
        date = finishesAt
      )
    }

  def nonLichessCreatedBy = (createdBy != UserId.lichess).option(createdBy)

  def ratingVariant = if variant.fromPosition then chess.variant.Standard else variant

  def startingPosition = position.flatMap(Thematic.byFen)

  lazy val prizeInDescription = lila.gathering.looksLikePrize(s"$name $description")
  lazy val looksLikePrize     = !isScheduled && prizeInDescription

  def estimateNumberOfGamesOneCanPlay: Double =
    // There are 2 players, and they don't always use all their time (0.8)
    // add 15 seconds for pairing delay
    val estimatedGameSeconds: Double = clock.estimateTotalSeconds * 2 * 0.8 + 15
    (minutes * 60) / estimatedGameSeconds

  override def toString =
    s"$id $startsAt $name $minutes minutes, $clock, $nbPlayers players"

case class EnterableTournaments(tours: List[Tournament], scheduled: List[Tournament])

object Tournament:
  val minPlayers = 2

  def fromSetup(setup: TournamentSetup)(using me: Me) =
    Tournament(
      id = makeId,
      name = setup.name | setup.realPosition.match
        case Some(pos) => Thematic.byFen(pos).fold("Custom position")(_.name.value)
        case None      => GreatPlayer.randomName
      ,
      status = Status.created,
      clock = setup.clockConfig,
      minutes = setup.minutes,
      createdBy = me.userId,
      createdAt = nowInstant,
      nbPlayers = 0,
      variant = setup.realVariant,
      position = setup.realPosition,
      rated = setup.realRated,
      password = setup.password,
      conditions = setup.conditions,
      teamBattle = setup.teamBattleByTeam.map(TeamBattle.init),
      noBerserk = !((setup.berserkable | true) && !setup.timeControlPreventsBerserk),
      noStreak = !(setup.streakable | true),
      schedule = None,
      startsAt =
        setup.startDate | nowInstant.plusMinutes(setup.waitMinutes | TournamentForm.waitMinuteDefault),
      description = setup.description,
      hasChat = setup.hasChat | true
    )

  def scheduleAs(sched: Schedule, startsAt: Instant, minutes: Int)(using Translate) =
    Tournament(
      id = makeId,
      name = TournamentName(sched, full = false),
      status = Status.created,
      clock = Schedule.clockFor(sched),
      minutes = minutes,
      createdBy = UserId.lichess,
      createdAt = nowInstant,
      nbPlayers = 0,
      variant = sched.variant,
      position = sched.position,
      rated = Rated.Yes,
      conditions = sched.conditions,
      schedule = Scheduled(sched.freq, sched.at).some,
      startsAt = startsAt
    )

  def tournamentUrl(tourId: TourId): String = s"https://lichess.org/tournament/$tourId"

  def makeId = TourId(ThreadLocalRandom.nextString(8))

  case class PastAndNext(past: List[Tournament], next: List[Tournament])

  enum JoinResult(val error: Option[String]):
    def ok = error.isEmpty
    case Ok             extends JoinResult(none)
    case WrongEntryCode extends JoinResult("Wrong entry code".some)
    case Paused         extends JoinResult("Your pause is not over yet".some)
    case Verdicts       extends JoinResult("Tournament restrictions".some)
    case MissingTeam    extends JoinResult("Missing team".some)
    case ArenaBanned    extends JoinResult("You are not allowed to join arenas".some)
    case PrizeBanned    extends JoinResult("You are not allowed to play in prized tournaments".some)
    case RateLimited    extends JoinResult("You are joining too many tournaments".some)
    case NotFound       extends JoinResult("This tournament no longer exists".some)
    case Nope           extends JoinResult("Couldn't join for some reason?".some)
  object JoinResult:
    given Zero[Tournament.JoinResult] = Zero(NotFound)
