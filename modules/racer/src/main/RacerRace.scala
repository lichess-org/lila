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
}

object RacerRace {

  case class Id(value: String) extends AnyVal with StringValue

  def make(owner: RacerPlayer.Id, puzzles: List[StormPuzzle]) = RacerRace(
    _id = Id(lila.common.ThreadLocalRandom nextString 8),
    owner = owner,
    players = List(
      RacerPlayer(
        id = owner,
        createdAt = DateTime.now
      )
    ),
    puzzles = puzzles,
    createdAt = DateTime.now,
    startsAt = none,
    finishedAt = none
  )
}
