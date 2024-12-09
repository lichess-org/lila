package lila.recap

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.ByColor
import chess.opening.OpeningFamily
import lila.common.SimpleOpening
import chess.format.pgn.SanStr
import lila.recap.Recap.Counted
import scalalib.model.Days
import lila.core.game.Source
import play.api.libs.json.JsObject

case class Recap(
    @Key("_id") id: UserId,
    year: Int,
    games: RecapGames,
    puzzles: RecapPuzzles,
    createdAt: Instant
):
  def userIds = games.opponents.map(_.value)

case class RecapGames(
    nb: NbAndStreak,
    nbWhite: Int,
    moves: Int,
    openings: Recap.Openings,
    firstMoves: List[Counted[SanStr]],
    results: Results,
    timePlaying: FiniteDuration,
    sources: Map[Source, Int],
    opponents: List[Counted[UserId]],
    perfs: List[Recap.Perf]
):
  def significantPerfs: List[Recap.Perf] = perfs.filter: p =>
    (p.games > (nb.nb / 20)) || (p.seconds > (timePlaying.toSeconds / 20))

case class RecapPuzzles(nb: NbAndStreak, results: Results, votes: PuzzleVotes)

object Recap:

  case class Counted[A](value: A, count: Int)
  case class Perf(key: PerfKey, seconds: Int, games: Int):
    def duration = seconds.seconds

  type Openings = ByColor[Counted[SimpleOpening]]
  lazy val nopening = Counted(SimpleOpening.openingList.head, 0)

  type QueueEntry = lila.memo.ParallelQueue.Entry[UserId]

  enum Availability:
    case Available(data: JsObject)
    case Queued(data: JsObject)

case class Results(win: Int = 0, draw: Int = 0, loss: Int = 0)
case class NbAndStreak(nb: Int, streak: Days)
case class PuzzleVotes(up: Int = 0, down: Int = 0, themes: Int = 0)
