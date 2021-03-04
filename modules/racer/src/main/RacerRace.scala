package lila.racer

import org.joda.time.DateTime

import lila.user.User
import lila.puzzle.Puzzle
import lila.storm.StormPuzzle

case class RacerRace(
    _id: RacerRace.Id,
    owner: RacerPlayer.Id,
    players: List[RacerPlayer],
    puzzleIds: List[Puzzle.Id],
    createdAt: DateTime,
    startsAt: Option[DateTime],
    finishedAt: Option[DateTime]
) {

  def id = _id
}

object RacerRace {

  case class Id(value: String) extends AnyVal with StringValue

  case class WithPuzzles(race: RacerRace, puzzles: List[StormPuzzle])

  def make(owner: RacerPlayer.Id, puzzleIds: List[Puzzle.Id]) = RacerRace(
    _id = Id(lila.common.ThreadLocalRandom nextString 8),
    owner = owner,
    players = List(
      RacerPlayer(
        id = owner,
        createdAt = DateTime.now
      )
    ),
    puzzleIds = puzzleIds,
    createdAt = DateTime.now,
    startsAt = none,
    finishedAt = none
  )
}
