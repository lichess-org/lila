package lila.tournament

import org.joda.time.{ DateTime, Duration }
import ornicar.scalalib.Random

import chess.{ Speed, Mode }
import lila.game.{ PovRef, PerfPicker }
import lila.user.User

private[tournament] case class Data(
  name: String,
  system: System,
  clock: TournamentClock,
  minutes: Int,
  variant: chess.variant.Variant,
  mode: Mode,
  `private`: Boolean,
  schedule: Option[Schedule],
  createdAt: DateTime,
  createdBy: String)

sealed trait Tournament {

  val id: String
  val data: Data
  def players: Players
  def winner: Option[Player]
  def pairings: List[Pairing]
  def events: List[Event]
  def isOpen: Boolean = false
  def isRunning: Boolean = false
  def isFinished: Boolean = false
  def `private`: Boolean = data.`private`
  def startsAt: Option[DateTime] = None

  def name = data.name
  def fullName = s"$name $system"

  def clock = data.clock
  def minutes = data.minutes
  lazy val extendedDuration = new Duration((minutes * 60 + 3) * 1000)

  def system = data.system
  def variant = data.variant
  def mode = data.mode
  def speed = Speed(clock.chessClock.some)
  def rated = mode.rated
  def schedule = data.schedule
  def scheduled = data.schedule.isDefined

  def perfType = PerfPicker.perfType(speed, variant, none)
  def perfLens = PerfPicker.mainOrDefault(speed, variant, none)

  def userIds = players map (_.id)
  def activePlayers = players filter (_.active)
  def activeUserIds = activePlayers map (_.id)
  def nbActiveUsers = players count (_.active)
  def withdrawnPlayers = players filterNot (_.active)
  def nbPlayers = players.size
  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")
  def contains(userId: String): Boolean = userIds contains userId
  def contains(user: User): Boolean = contains(user.id)
  def contains(user: Option[User]): Boolean = ~user.map(contains)
  def isActive(userId: String): Boolean = activeUserIds contains userId
  def isActive(user: User): Boolean = isActive(user.id)
  def isActive(user: Option[User]): Boolean = ~user.map(isActive)
  def playerByUserId(userId: String) = players find (_.id == userId)

  lazy val rankedPlayers: RankedPlayers = system.scoringSystem.rank(this, players)
  def rankedPlayerByUserId(userId: String): Option[RankedPlayer] = rankedPlayers find {
    _.player.id == userId
  }

  def createdBy = data.createdBy
  def createdAt = data.createdAt

  def isCreator(userId: String) = createdBy == userId

  def userPairings(user: String) = pairings filter (_ contains user)

  def playingPairings = pairings filter (_.playing)

  def playingUserIds = playingPairings.flatMap(_.users).distinct

  def scoreSheet(player: Player) = system.scoringSystem.scoreSheet(this, player.id)

  def isSwiss = system == System.Swiss

  def pairingOfGameId(gameId: String) = pairings find { p => p.gameId == gameId }

  def berserkable = system.berserkable && clock.increment == 0

  // Oldest first!
  def pairingsAndEvents: List[Either[Pairing, Event]] =
    (pairings.reverse.map(Left(_)) ::: events.map(Right(_))).sorted(Tournament.PairingEventOrdering)
}

sealed trait Enterable extends Tournament {

  def withPlayers(s: Players): Enterable

  def join(user: User): Valid[Enterable]

  def withdraw(userId: String): Valid[Enterable]

  def joinNew(user: User): Valid[Enterable] = contains(user).fold(
    !!("User %s is already part of the tournament" format user.id),
    withPlayers(players :+ Player.make(user, perfLens)).success
  )

  def ejectCheater(userId: String): Option[Enterable] =
    activePlayers.find(_.id == userId) map { player =>
      withPlayers(players map {
        case p if p is player => p.doWithdraw
        case p                => p
      })
    }
}

sealed trait StartedOrFinished extends Tournament {

  def startedAt: DateTime
  def withPlayers(s: Players): StartedOrFinished
  def refreshPlayers: StartedOrFinished

  def winner = players.headOption
  def winnerUserId = winner map (_.id)

  def recentGameIds(max: Int) = pairings take max map (_.gameId)

  def finishedAt = startedAt plus extendedDuration
}

case class Created(
    id: String,
    data: Data,
    players: Players,
    waitMinutes: Option[Int]) extends Tournament with Enterable {

  import data._

  override def isOpen = true

  def enoughPlayersToStart = !scheduled && nbPlayers >= Tournament.minPlayers

  def isEmpty = players.isEmpty

  def pairings = Nil

  def events = Nil

  def winner = None

  def withdraw(userId: String): Valid[Created] = contains(userId).fold(
    withPlayers(players filterNot (_ is userId)).success,
    !!("User %s is not part of the tournament" format userId)
  )

  def withPlayers(s: Players) = copy(players = s)

  def startIfReady = enoughPlayersToStart option start

  def start = Started(id, data, DateTime.now, players, Nil, Nil)

  def asScheduled = schedule map { Scheduled(this, _) }

  def join(user: User) = joinNew(user)

  def waitUntil: Option[DateTime] = waitMinutes map createdAt.plusMinutes

  override def startsAt: Option[DateTime] = schedule.map(_.at).orElse(waitUntil)

  def hasWaitedEnough = startsAt.??(DateTime.now.isAfter)

  def secondsToStart: Option[Int] = startsAt.map { date =>
    (date.getSeconds - nowSeconds).toInt
  }
}

case class Scheduled(tour: Created, schedule: Schedule) {

  def endsAt = schedule.at plus (tour.minutes.toLong * 60 * 1000)
  def interval = new org.joda.time.Interval(schedule.at, endsAt)
  def overlaps(other: Scheduled) = interval overlaps other.interval
}

case class EnterableTournaments(tours: List[Created], scheduled: List[Created])

case class Started(
    id: String,
    data: Data,
    startedAt: DateTime,
    players: Players,
    pairings: List[Pairing],
    events: List[Event]) extends StartedOrFinished with Enterable {

  override def isRunning = true

  def addPairings(ps: scalaz.NonEmptyList[Pairing]) =
    copy(pairings = ps.list ::: pairings)

  def updatePairing(gameId: String, f: Pairing => Pairing) = copy(
    pairings = pairings map { p => if (p.gameId == gameId) f(p) else p }
  )

  def addEvents(es: Events) =
    copy(events = es ::: events)

  def readyToFinish = (remainingSeconds == 0) || (!scheduled && nbActiveUsers < 2)

  def remainingSeconds: Float = math.max(0f,
    ((finishedAt.getMillis - nowMillis) / 1000).toFloat
  )

  def isAlmostFinished = remainingSeconds < math.max(40, math.min(clock.limit / 2, 120))

  def clockStatus = remainingSeconds.toInt |> { s =>
    "%02d:%02d".format(s / 60, s % 60)
  }

  def userCurrentPairing(userId: String): Option[Pairing] = pairings find { p =>
    p.playing && p.contains(userId)
  }

  def userCurrentPov(userId: String): Option[PovRef] =
    userCurrentPairing(userId) flatMap (_ povRef userId)

  def userCurrentPov(user: Option[User]): Option[PovRef] =
    user.flatMap(u => userCurrentPov(u.id))

  def finish = refreshPlayers |> { tour =>
    Finished(
      id = tour.id,
      data = tour.data,
      startedAt = tour.startedAt,
      players = tour.players,
      pairings = tour.pairings filterNot (_.playing),
      events = tour.events)
  }

  def withdraw(userId: String): Valid[Started] = contains(userId).fold(
    withPlayers(players map {
      case p if p is userId => p.doWithdraw
      case p                => p
    }).success,
    !!("User %s is not part of the tournament" format userId)
  )

  def quickLossStreak(user: String): Boolean =
    userPairings(user).takeWhile { pair => (pair lostBy user) && pair.quickFinish }.size >= 3

  def withPlayers(s: Players) = copy(players = s)
  def refreshPlayers = withPlayers(Player refresh this)

  def join(user: User) = joinNew(user) orElse joinBack(user)

  private def joinBack(user: User) = withdrawnPlayers.find(_ is user) match {
    case None => !!("User %s is already part of the tournament" format user.id)
    case Some(player) => withPlayers(players map {
      case p if p is player => p.unWithdraw
      case p                => p
    }).success
  }
}

case class Finished(
    id: String,
    data: Data,
    startedAt: DateTime,
    players: Players,
    pairings: List[Pairing],
    events: List[Event]) extends StartedOrFinished {

  override def isFinished = true

  def withPlayers(s: Players) = copy(players = s)
  def refreshPlayers = withPlayers(Player refresh this)
}

object Tournament {

  val minPlayers = 3

  def make(
    createdBy: User,
    clock: TournamentClock,
    minutes: Int,
    waitMinutes: Int,
    system: System,
    variant: chess.variant.Variant,
    mode: Mode,
    `private`: Boolean): Created = {
    val tour = Created(
      id = Random nextStringUppercase 8,
      data = Data(
        name = RandomName(),
        system = system,
        clock = clock,
        createdBy = createdBy.id,
        createdAt = DateTime.now,
        variant = variant,
        mode = mode,
        `private` = `private`,
        minutes = minutes,
        schedule = None),
      players = Nil,
      waitMinutes = waitMinutes.some)
    tour withPlayers List(Player.make(createdBy, tour.perfLens))
  }

  def schedule(sched: Schedule, minutes: Int) = Created(
    id = Random nextStringUppercase 8,
    data = Data(
      name = sched.name,
      system = System.default,
      clock = Schedule clockFor sched,
      createdBy = "lichess",
      createdAt = DateTime.now,
      variant = sched.variant,
      mode = Mode.Rated,
      `private` = false,
      minutes = minutes,
      schedule = Some(sched)),
    players = List(),
    waitMinutes = None)

  // To sort combined sequences of pairings and events.
  // This sorts all pairings/events in chronological order. Pairings without a timestamp
  // are assumed to have happened at the origin of time (i.e. before anything else).
  object PairingEventOrdering extends Ordering[Either[Pairing, Event]] {
    def compare(x: Either[Pairing, Event], y: Either[Pairing, Event]): Int = {
      val ot1: Option[DateTime] = x.fold(_.pairedAt, e => Some(e.timestamp))
      val ot2: Option[DateTime] = y.fold(_.pairedAt, e => Some(e.timestamp))

      (ot1, ot2) match {
        case (None, None)         => 0
        case (None, Some(_))      => -1
        case (Some(_), None)      => 1
        case (Some(t1), Some(t2)) => if (t1 equals t2) 0 else if (t1 isBefore t2) -1 else 1
      }
    }
  }
}
