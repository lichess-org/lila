package lila.racer

import org.joda.time.DateTime

import lila.user.User
import lila.puzzle.Puzzle
import lila.storm.StormPuzzle

case class RacerRace(
    _id: RacerRace.Id,
    owner: RacerPlayer.Id,
    players: List[RacerPlayer],
    puzzles: List[StormPuzzle],
    startsAt: Option[DateTime],
    rematch: Option[RacerRace.Id]
) {

  def id = _id

  def has(id: RacerPlayer.Id) = players.exists(_.id == id)

  def player(id: RacerPlayer.Id) = players.find(_.id == id)

  def join(id: RacerPlayer.Id): Option[RacerRace] =
    !hasStarted && !has(id) && players.sizeIs < RacerRace.maxPlayers option
      copy(players = players :+ RacerPlayer.make(id))

  def registerScore(playerId: RacerPlayer.Id, score: Int): Option[RacerRace] =
    !finished option copy(
      players = players map {
        case p if p.id == playerId => p.copy(score = score)
        case p                     => p
      }
    )

  def startCountdown: Option[RacerRace] =
    startsAt.isEmpty && players.size > (if (isLobby) 2 else 1) option
      copy(startsAt = DateTime.now.plusSeconds(if (isLobby) 7 else 10).some)

  def startsInMillis = startsAt.map(d => d.getMillis - nowMillis)

  def hasStarted = startsInMillis.exists(_ <= 0)

  def finishesAt = startsAt.map(_ plusSeconds RacerRace.duration)

  def finished = finishesAt.exists(_.isBeforeNow)

  def isLobby = owner == RacerPlayer.lichess
}

object RacerRace {

  val duration   = 90
  val maxPlayers = 10

  case class Id(value: String) extends AnyVal with StringValue

  def make(owner: RacerPlayer.Id, puzzles: List[StormPuzzle]) = RacerRace(
    _id = Id(lila.common.ThreadLocalRandom nextString 8),
    owner = owner,
    players = Nil,
    puzzles = puzzles,
    startsAt = none,
    rematch = none
  )
}
