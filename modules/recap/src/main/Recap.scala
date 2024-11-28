package lila.recap

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.ByColor
import chess.opening.OpeningFamily
import lila.common.SimpleOpening
import chess.format.pgn.SanStr
import lila.recap.Recap.Counted
import scalalib.model.Days

case class Recap(
    @Key("_id") id: UserId,
    year: Int,
    nbGames: Int,
    streakDays: Days,
    openings: Recap.Openings,
    firstMove: Option[Counted[SanStr]],
    results: Recap.Results,
    timePlaying: FiniteDuration,
    opponent: Option[Counted[UserId]],
    perfs: List[Recap.Perf],
    createdAt: Instant
):
  def significantPerfs: List[Recap.Perf] = perfs.filter: p =>
    (p.games > (nbGames / 20)) || (p.seconds > (timePlaying.toSeconds / 20))

object Recap:

  case class Counted[A](value: A, count: Int)
  case class Perf(key: PerfKey, seconds: Int, games: Int):
    def duration = seconds.seconds

  type Openings = ByColor[Counted[SimpleOpening]]
  lazy val nopening = Counted(SimpleOpening.openingList.head, 0)

  case class Results(win: Int, draw: Int, loss: Int)

  type QueueEntry = lila.memo.ParallelQueue.Entry[UserId]

  enum Availability:
    case Available(recap: Recap)
    case Queued(entry: QueueEntry)
    case InsufficientGames
