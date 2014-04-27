package lila.tournament

import com.github.nscala_time.time.Imports._
import org.joda.time.{ DateTime, Duration }
import ornicar.scalalib.Random

import chess.{ Variant, Mode }
import lila.game.PovRef
import lila.user.User

private[tournament] case class Data(
  name: String,
  clock: TournamentClock,
  minutes: Int,
  minPlayers: Int,
  variant: Variant,
  mode: Mode,
  password: Option[String],
  schedule: Option[Schedule],
  createdAt: DateTime,
  createdBy: String)

sealed trait Tournament {

  val id: String
  val data: Data
  def encode: RawTournament
  def players: Players
  def winner: Option[Player]
  def pairings: List[Pairing]
  def isOpen: Boolean = false
  def isRunning: Boolean = false
  def isFinished: Boolean = false

  def numerotedPairings: Seq[(Int, Pairing)] = (pairings.size to 1 by -1) zip pairings

  def name = data.name

  def clock = data.clock
  def minutes = data.minutes
  lazy val duration = new Duration(minutes * 60 * 1000)

  def variant = data.variant
  def mode = data.mode
  def rated = mode.rated
  def password = data.password
  def hasPassword = password.isDefined
  def schedule = data.schedule
  def scheduled = data.schedule.isDefined

  def userIds = players map (_.id)
  def activePlayers = players filter (_.active)
  def activeUserIds = activePlayers map (_.id)
  def nbActiveUsers = players count (_.active)
  def withdrawnPlayers = players filterNot (_.active)
  def nbPlayers = players.size
  def minPlayers = data.minPlayers
  def playerRatio = if (scheduled) nbPlayers.toString else s"$nbPlayers/$minPlayers"
  def durationString =
    if (minutes < 60) s"${minutes}m"
    else s"${minutes / 60}h" + (if (minutes % 60 != 0) s" ${(minutes % 60)}m" else "")
  def contains(userId: String): Boolean = userIds contains userId
  def contains(user: User): Boolean = contains(user.id)
  def contains(user: Option[User]): Boolean = ~user.map(contains)
  def isActive(userId: String): Boolean = activeUserIds contains userId
  def isActive(user: User): Boolean = isActive(user.id)
  def isActive(user: Option[User]): Boolean = ~user.map(isActive)
  def missingPlayers = minPlayers - players.size

  def createdBy = data.createdBy
  def createdAt = data.createdAt

  def isCreator(userId: String) = data.createdBy == userId

  def userPairings(user: String) = pairings filter (_ contains user)

  def scoreSheet(player: Player) = Score.sheet(player.id, this)
}

sealed trait Enterable extends Tournament {

  def withPlayers(s: Players): Enterable

  def join(user: User, pass: Option[String]): Valid[Enterable]

  def withdraw(userId: String): Valid[Enterable]

  def joinNew(user: User, pass: Option[String]): Valid[Enterable] = contains(user).fold(
    !!("User %s is already part of the tournament" format user.id),
    (pass != password).fold(
      !!("Invalid tournament password"),
      withPlayers(players :+ Player.make(user)).success
    ))

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

  type RankedPlayers = List[(Int, Player)]
  def rankedPlayers: RankedPlayers = players.foldLeft(Nil: RankedPlayers) {
    case (Nil, p)                  => (1, p) :: Nil
    case (list@((r0, p0) :: _), p) => ((p0.score == p.score).fold(r0, r0 + 1), p) :: list
  }.reverse

  def winner = players.headOption
  def winnerUserId = winner map (_.username)

  def playingPairings = pairings filter (_.playing)
  def recentGameIds(max: Int) = pairings take max map (_.gameId)

  def encode(status: Status) = new RawTournament(
    id = id,
    status = status.id,
    name = data.name,
    clock = data.clock,
    minutes = data.minutes,
    minPlayers = data.minPlayers,
    variant = data.variant.id,
    mode = data.mode.id,
    password = data.password,
    createdAt = data.createdAt,
    createdBy = data.createdBy,
    startedAt = startedAt.some,
    schedule = data.schedule,
    players = players,
    pairings = pairings map (_.encode))

  def finishedAt = startedAt + duration
}

case class Created(
    id: String,
    data: Data,
    players: Players) extends Tournament with Enterable {

  import data._

  override def isOpen = true

  def enoughPlayersToStart = !scheduled && nbPlayers >= minPlayers

  def enoughPlayersToEarlyStart = !scheduled && nbPlayers >= Tournament.minPlayers

  def isEmpty = players.isEmpty

  def pairings = Nil

  def winner = None

  def encode = new RawTournament(
    id = id,
    status = Status.Created.id,
    name = data.name,
    clock = data.clock,
    variant = data.variant.id,
    mode = data.mode.id,
    password = data.password,
    minutes = data.minutes,
    minPlayers = data.minPlayers,
    schedule = data.schedule,
    createdAt = data.createdAt,
    createdBy = data.createdBy,
    players = players)

  def withdraw(userId: String): Valid[Created] = contains(userId).fold(
    withPlayers(players filterNot (_ is userId)).success,
    !!("User %s is not part of the tournament" format userId)
  )

  def withPlayers(s: Players) = copy(players = s)

  def startIfReady = enoughPlayersToStart option start

  def start = Started(id, data, DateTime.now, players, Nil)

  def asScheduled = schedule map { Scheduled(this, _) }

  def join(user: User, pass: Option[String]) = joinNew(user, pass)
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
    pairings: List[Pairing]) extends StartedOrFinished with Enterable {

  override def isRunning = true

  def addPairings(ps: scalaz.NonEmptyList[Pairing]) =
    copy(pairings = ps.list ::: pairings)

  def updatePairing(gameId: String, f: Pairing => Pairing) = copy(
    pairings = pairings map { p => (p.gameId == gameId).fold(f(p), p) }
  )

  def readyToFinish = (remainingSeconds == 0) || (nbActiveUsers < 2)

  def remainingSeconds: Float = math.max(0f,
    ((finishedAt.getMillis - nowMillis) / 1000).toFloat
  )

  def clockStatus = remainingSeconds.toInt |> { s =>
    "%02d:%02d".format(s / 60, s % 60)
  }

  def userCurrentPov(userId: String): Option[PovRef] =
    playingPairings.map { _ povRef userId }.flatten.headOption

  def userCurrentPov(user: Option[User]): Option[PovRef] =
    user.flatMap(u => userCurrentPov(u.id))

  def leaders: List[Player] = rankedPlayers filter {
    case (rank, player) => rank <= 2 && player.score >= 8
  } map (_._2)

  def finish = refreshPlayers |> { tour =>
    Finished(
      id = tour.id,
      data = tour.data,
      startedAt = tour.startedAt,
      players = tour.players,
      pairings = tour.pairings filterNot (_.playing))
  }

  def withdraw(userId: String): Valid[Started] = contains(userId).fold(
    withPlayers(players map {
      case p if p is userId => p.doWithdraw
      case p                => p
    }).success,
    !!("User %s is not part of the tournament" format userId)
  )

  def quickLossStreak(user: String): Boolean =
    userPairings(user).takeWhile { pair => (pair lostBy user) && pair.quickLoss }.size >= 3

  def withPlayers(s: Players) = copy(players = s)
  def refreshPlayers = withPlayers(Player refresh this)

  def encode = refreshPlayers.encode(Status.Started)

  def join(user: User, pass: Option[String]) = joinNew(user, pass) orElse joinBack(user, pass)

  private def joinBack(user: User, pass: Option[String]) = withdrawnPlayers.find(_ is user) match {
    case None => !!("User %s is already part of the tournament" format user.id)
    case Some(player) => (pass != password).fold(
      !!("Invalid tournament password"),
      withPlayers(players map {
        case p if p is player => p.unWithdraw
        case p                => p
      }).success)
  }
}

case class Finished(
    id: String,
    data: Data,
    startedAt: DateTime,
    players: Players,
    pairings: List[Pairing]) extends StartedOrFinished {

  override def isFinished = true

  def withPlayers(s: Players) = copy(players = s)
  def refreshPlayers = withPlayers(Player refresh this)

  def encode = encode(Status.Finished)
}

object Tournament {

  val minPlayers = 4

  import lila.db.JsTube
  import play.api.libs.json._

  private def reader[T <: Tournament](decode: RawTournament => Option[T])(js: JsValue): JsResult[T] = ~(for {
    obj ← js.asOpt[JsObject]
    rawTour ← RawTournament.tube.read(obj).asOpt
    tour ← decode(rawTour)
  } yield JsSuccess(tour): JsResult[T])

  private lazy val writer = Writes[Tournament](tour =>
    RawTournament.tube.write(tour.encode) getOrElse JsUndefined("[db] Can't write tournament " + tour.id)
  )

  private[tournament] val tube: JsTube[Tournament] =
    JsTube(Reads(reader(_.decode)), writer)

  private[tournament] val enterableTube: JsTube[Enterable] =
    JsTube(Reads(reader(_.enterable)), writer)

  private[tournament] val createdTube: JsTube[Created] =
    JsTube(Reads(reader(_.created)), writer)

  private[tournament] val startedTube: JsTube[Started] =
    JsTube(Reads(reader(_.started)), writer)

  private[tournament] val finishedTube: JsTube[Finished] =
    JsTube(Reads(reader(_.finished)), writer)

  def make(
    createdBy: User,
    clock: TournamentClock,
    minutes: Int,
    minPlayers: Int,
    variant: Variant,
    mode: Mode,
    password: Option[String]): Created = Created(
    id = Random nextStringUppercase 8,
    data = Data(
      name = RandomName(),
      clock = clock,
      createdBy = createdBy.id,
      createdAt = DateTime.now,
      variant = variant,
      mode = mode,
      password = password,
      minutes = minutes,
      schedule = None,
      minPlayers = minPlayers),
    players = List(Player make createdBy))

  def schedule(sched: Schedule, minutes: Int) = Created(
    id = Random nextStringUppercase 8,
    data = Data(
      name = sched.name,
      clock = Schedule clockFor sched,
      createdBy = "lichess",
      createdAt = DateTime.now,
      variant = Variant.Standard,
      mode = Mode.Rated,
      password = None,
      minutes = minutes,
      schedule = Some(sched),
      minPlayers = 0),
    players = List())
}

private[tournament] case class RawTournament(
    id: String,
    name: String,
    clock: TournamentClock,
    minutes: Int,
    minPlayers: Int,
    password: Option[String] = None,
    createdAt: DateTime,
    createdBy: String,
    status: Int,
    startedAt: Option[DateTime] = None,
    players: List[Player] = Nil,
    pairings: List[RawPairing] = Nil,
    variant: Int = Variant.Standard.id,
    mode: Int = Mode.Casual.id,
    schedule: Option[Schedule] = None) {

  def decode: Option[Tournament] = created orElse started orElse finished

  def created: Option[Created] = (status == Status.Created.id) option Created(
    id = id,
    data = data,
    players = players)

  def started: Option[Started] = for {
    stAt ← startedAt
    if status == Status.Started.id
  } yield Started(
    id = id,
    data = data,
    startedAt = stAt,
    players = players,
    pairings = decodePairings)

  def finished: Option[Finished] = for {
    stAt ← startedAt
    if status == Status.Finished.id
  } yield Finished(
    id = id,
    data = data,
    startedAt = stAt,
    players = players,
    pairings = decodePairings)

  def enterable: Option[Enterable] = created orElse started

  private def data = Data(
    name,
    clock,
    minutes,
    minPlayers,
    Variant orDefault variant,
    Mode orDefault mode,
    password,
    schedule,
    createdAt,
    createdBy)

  private def decodePairings = pairings map (_.decode) flatten

  def any: Option[Tournament] = Status(status) flatMap {
    case Status.Created  => created
    case Status.Started  => started
    case Status.Finished => finished
  }
}

private[tournament] object RawTournament {

  import lila.db.JsTube
  import JsTube.Helpers._
  import play.api.libs.json._

  private implicit def pairingTube = RawPairing.tube
  private implicit def clockTube = TournamentClock.tube
  private implicit def scheduleTube = Schedule.tube
  private implicit def PlayerTube = Player.tube

  private def defaults = Json.obj(
    "password" -> none[String],
    "startedAt" -> none[DateTime],
    "players" -> List[Player](),
    "pairings" -> List[RawPairing](),
    "variant" -> Variant.Standard.id,
    "mode" -> Mode.Casual.id,
    "schedule" -> none[Schedule])

  private[tournament] val tube = JsTube(
    (__.json update (
      merge(defaults) andThen readDate('createdAt) andThen readDateOpt('startedAt)
    )) andThen Json.reads[RawTournament],
    Json.writes[RawTournament] andThen (__.json update (
      writeDate('createdAt) andThen writeDateOpt('startedAt)
    ))
  )
}
