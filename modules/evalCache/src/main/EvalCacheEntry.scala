package lila.evalCache

import chess.format.{ Fen, Uci }
import chess.variant.Variant
import org.joda.time.DateTime
import cats.data.NonEmptyList

import lila.tree.Eval.Score
import lila.user.User

case class EvalCacheEntry(
    _id: EvalCacheEntry.Id,
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[EvalCacheEntry.Eval],
    usedAt: DateTime,
    updatedAt: DateTime
):

  import EvalCacheEntry.*

  inline def id = _id

  def add(eval: Eval) =
    copy(
      evals = EvalCacheSelector(eval :: evals),
      usedAt = DateTime.now,
      updatedAt = DateTime.now
    )

  // finds the best eval with at least multiPv pvs,
  // and truncates its pvs to multiPv
  def makeBestMultiPvEval(multiPv: Int): Option[Eval] =
    evals
      .find(_.multiPv >= multiPv.atMost(nbMoves))
      .map(_ takePvs multiPv)

  def similarTo(other: EvalCacheEntry) =
    id == other.id && evals == other.evals

object EvalCacheEntry:

  case class Eval(
      pvs: NonEmptyList[Pv],
      knodes: Knodes,
      depth: Int,
      by: UserId,
      trust: Trust
  ):

    def multiPv = pvs.size

    def bestPv: Pv = pvs.head

    def bestMove: Uci = bestPv.moves.value.head

    def looksValid =
      pvs.toList.forall(_.looksValid) && {
        pvs.toList.forall(_.score.mateFound) || (knodes >= MIN_KNODES || depth >= MIN_DEPTH)
      }

    def truncatePvs = copy(pvs = pvs.map(_.truncate))

    def takePvs(multiPv: Int) =
      copy(
        pvs = NonEmptyList(pvs.head, pvs.tail.take(multiPv - 1))
      )

    def depthAboveMin = (depth - MIN_DEPTH) atLeast 0

  case class Pv(score: Score, moves: Moves):

    def looksValid =
      score.mate match
        case None       => moves.value.toList.sizeIs > MIN_PV_SIZE
        case Some(mate) => mate.value != 0 // sometimes we get #0. Dunno why.

    def truncate = copy(moves = Moves truncate moves)

  case class TrustedUser(trust: Trust, user: User)

  case class Id(variant: Variant, smallFen: SmallFen)

  case class Input(id: Id, fen: Fen.Epd, eval: Eval)

  object Input:
    case class Candidate(variant: Variant, fen: Fen.Epd, eval: Eval):
      def input =
        SmallFen.validate(variant, fen) ifTrue eval.looksValid map { smallFen =>
          Input(Id(variant, smallFen), fen, eval.truncatePvs)
        }
