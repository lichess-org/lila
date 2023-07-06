package lila.evalCache

import chess.variant.Variant
import lila.tree.Score

case class EvalCacheEntry(
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[EvalCacheEntry.Eval]
):
  import EvalCacheEntry.*

  // finds the best eval with at least multiPv pvs,
  // and truncates its pvs to multiPv.
  // Defaults to lower multiPv if no eval has enough pvs.
  def makeBestMultiPvEval(multiPv: MultiPv): Option[Eval] =
    evals
      .find(_.multiPv >= multiPv.atMost(nbMoves))
      .map(_ takePvs multiPv)
      .orElse:
        evals.sortBy(-_.multiPv.value).headOption

object EvalCacheEntry:

  case class Eval(pvs: NonEmptyList[Pv], knodes: Knodes, depth: Depth, by: UserId):

    def multiPv    = MultiPv(pvs.size)
    def bestPv: Pv = pvs.head
    def takePvs(multiPv: MultiPv) =
      copy(pvs = NonEmptyList(pvs.head, pvs.tail.take(multiPv.value - 1)))

  case class Pv(score: Score, moves: Moves)

  case class Id(variant: Variant, smallFen: SmallFen)
