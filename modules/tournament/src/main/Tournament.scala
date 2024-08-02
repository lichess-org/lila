package lila.tournament

import chess.Clock.Config as ClockConfig
import chess.format.Fen
import chess.{ Mode, Speed }
import scalalib.ThreadLocalRandom

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
    mode: Mode,
    password: Option[String] = None,
    conditions: TournamentCondition.All,
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
) extends lila.core.tournament.Tournament:

  def isCreated   = status == Status.Created
  def isStarted   = status == Status.Started
  def isFinished  = status == Status.Finished
  def isEnterable = !isFinished

  def isPrivate = password.isDefined

  def isTeamBattle  = teamBattle.isDefined
  def isTeamRelated = isTeamBattle || conditions.teamMember.isDefined

  def name(full: Boolean = true)(using Translate): String =
    if isMarathon || isUnique then name
    else if isTeamBattle && full then lila.core.i18n.I18nKey.tourname.xTeamBattle.txt(name)
    else if isTeamBattle then name
    else schedule.fold(if full then s"$name Arena" else name)(_.name(full))

  def isMarathon =
    schedule.map(_.freq).exists {
      case Schedule.Freq.ExperimentalMarathon | Schedule.Freq.Marathon => true
      case _                                                           => false
    }

  def isShield = schedule.map(_.freq).has(Schedule.Freq.Shield)

  def isUnique = schedule.map(_.freq).has(Schedule.Freq.Unique)

  def isMarathonOrUnique = isMarathon || isUnique

  def isScheduled = schedule.isDefined

  def isRated = mode == Mode.Rated

  def finishesAt = startsAt.plusMinutes(minutes)

  def imminentStart = isCreated && (startsAt.toMillis - nowMillis) < 1000

  def secondsToStart = (startsAt.toSeconds - nowSeconds).toInt.atLeast(0)

  def secondsToFinish = (finishesAt.toSeconds - nowSeconds).toInt.atLeast(0)

  def pairingsClosed = secondsToFinish < math.max(30, math.min(clock.limitSeconds.value / 2, 120))

  def isStillWorthEntering =
    isMarathonOrUnique || {
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

  def similarTo(other: Tournament) =
    (schedule, other.schedule) match
      case (Some(s1), Some(s2)) if s1.similarTo(s2) => true
      case _                                        => false

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
    secondsToFinish.pipe: s =>
      "%02d:%02d".format(s / 60, s % 60)

  def schedulePair = schedule.map { this -> _ }

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
      status = Status.Created,
      clock = setup.clockConfig,
      minutes = setup.minutes,
      createdBy = me.userId,
      createdAt = nowInstant,
      nbPlayers = 0,
      variant = setup.realVariant,
      position = setup.realPosition,
      mode = setup.realMode,
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

  def scheduleAs(sched: Schedule, minutes: Int)(using Translate) =
    Tournament(
      id = makeId,
      name = sched.name(full = false),
      status = Status.Created,
      clock = Schedule.clockFor(sched),
      minutes = minutes,
      createdBy = UserId.lichess,
      createdAt = nowInstant,
      nbPlayers = 0,
      variant = sched.variant,
      position = sched.position,
      mode = Mode.Rated,
      conditions = sched.conditions,
      schedule = Some(sched),
      startsAt = sched.at.instant.plusSeconds(ThreadLocalRandom.nextInt(60))
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
    case Nope           extends JoinResult("Couldn't join for some reason?".some)
