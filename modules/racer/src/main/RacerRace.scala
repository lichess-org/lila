package lila.racer

import scalalib.ThreadLocalRandom
import scalalib.model.Seconds

import lila.storm.StormPuzzle

case class RacerRace(
    _id: RacerRace.Id,
    owner: RacerPlayer.Id,
    duration: Seconds,
    players: List[RacerPlayer],
    puzzles: List[StormPuzzle],
    countdown: Seconds,
    startsAt: Option[Instant],
    rematch: Option[RacerRace.Id]
):

  inline def id = _id

  def has(id: RacerPlayer.Id) = players.exists(_.id == id)

  def player(id: RacerPlayer.Id) = players.find(_.id == id)

  def ownerName =
    players.find(_.id == owner).fold(if isLobby then UserName.lichess else UserName.anonymous)(_.name)

  def join(player: RacerPlayer): Option[RacerRace] =
    (!hasStarted && !has(player.id) && players.sizeIs < RacerRace.maxPlayers)
      .option(copy(players = players :+ player))

  def registerScore(playerId: RacerPlayer.Id, score: Int): Option[RacerRace] =
    (!finished).option:
      copy(
        players = players.map: p =>
          if p.id == playerId then p.copy(score = score)
          else p
      )

  def startCountdown: Option[RacerRace] =
    (startsAt.isEmpty && players.size > (if isLobby then 2 else 1))
      .option(copy(startsAt = nowInstant.plus(countdown.duration).some))

  def startsInMillis = startsAt.map(d => d.toMillis - nowMillis)

  def hasStarted = startsInMillis.exists(_ <= 0)

  def finishesAt = startsAt.map(_.plus(duration.duration))

  def finished = finishesAt.exists(_.isBeforeNow)

  def isLobby = owner == RacerPlayer.lichess

object RacerRace:

  val maxPlayers = 10

  opaque type Id = String
  object Id extends OpaqueString[Id]

  def make(owner: RacerPlayer.Id, puzzles: List[StormPuzzle], countdown: Seconds) = RacerRace(
    _id = Id(ThreadLocalRandom.nextString(5)),
    owner = owner,
    duration = Seconds(90),
    players = Nil,
    puzzles = puzzles,
    countdown = countdown,
    startsAt = none,
    rematch = none
  )
