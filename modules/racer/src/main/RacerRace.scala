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

  def player(id: RacerPlayer.Id) = players.find(_.id == id)

  def join(id: RacerPlayer.Id): Option[RacerRace] =
    !has(id) && players.sizeIs <= RacerRace.maxPlayers option
      copy(players = players :+ RacerPlayer.make(id)).startCountdown

  def registerMoves(playerId: RacerPlayer.Id, moves: Int): RacerRace =
    copy(
      players = players map {
        case p if p.id == playerId => p.copy(moves = moves)
        case p                     => p
      }
    )

  def startCountdown =
    if (startsAt.isEmpty) copy(startsAt = DateTime.now.plusSeconds(5).some)
    else this

  def startsInMillis = startsAt.map(d => d.getMillis - nowMillis)

  def hasStarted = startsInMillis.exists(_ <= 0)

  lazy val moves = puzzles.foldLeft(0) { case (m, p) =>
    m + p.line.size / 2
  }
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
