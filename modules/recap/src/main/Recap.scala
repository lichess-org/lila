package lila.recap

import reactivemongo.api.bson.Macros.Annotations.Key
import chess.ByColor
import chess.opening.OpeningFamily
import lila.common.SimpleOpening
import chess.format.pgn.SanStr

case class Recap(
    @Key("_id") id: UserId,
    nbGames: Int,
    openings: Recap.Openings,
    firstMove: Option[Recap.Counted[SanStr]],
    results: Recap.Results,
    timePlaying: FiniteDuration,
    opponent: Option[Recap.Counted[UserId]],
    createdAt: Instant
)

object Recap:

  case class Counted[A](value: A, count: Int)

  type Openings = ByColor[Counted[SimpleOpening]]
  lazy val nopening = Counted(SimpleOpening.openingList.head, 0)

  case class Results(win: Int, draw: Int, loss: Int)

  type QueueEntry = lila.memo.ParallelQueue.Entry[UserId]

  enum Availability:
    case Available(recap: Recap)
    case Queued(entry: QueueEntry)
    case InsufficientGames
