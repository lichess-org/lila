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
    createdAt: DateTime,
    startsAt: Option[DateTime],
    finishedAt: Option[DateTime]
) {

  def id = _id

  def has(id: RacerPlayer.Id) = players.exists(_.id == id)

  def join(id: RacerPlayer.Id): Option[RacerRace] =
    !has(id) && players.sizeIs <= RacerRace.maxPlayers option
      copy(players = players :+ RacerPlayer.make(id))
}

object RacerRace {

  val maxPlayers = 10

  case class Id(value: String) extends AnyVal with StringValue

  def make(owner: RacerPlayer.Id, puzzles: List[StormPuzzle]) = RacerRace(
    _id = Id(lila.common.ThreadLocalRandom nextString 8),
    owner = owner,
    players = List(
      RacerPlayer.make(owner)
    ),
    puzzles = puzzles,
    createdAt = DateTime.now,
    startsAt = none,
    finishedAt = none
  )
}
