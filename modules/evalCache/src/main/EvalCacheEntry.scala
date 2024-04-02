package lila.evalCache

import chess.variant.Variant

import lila.tree.CloudEval

case class EvalCacheEntry(
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[CloudEval]
):
  import EvalCacheEntry.*

  // finds the best eval with at least multiPv pvs,
  // and truncates its pvs to multiPv.
  // Defaults to lower multiPv if no eval has enough pvs.
  def makeBestMultiPvEval(multiPv: MultiPv): Option[CloudEval] =
    evals
      .find(_.multiPv >= multiPv.atMost(nbMoves))
      .map(_.takePvs(multiPv))
      .orElse:
        evals.sortBy(-_.multiPv.value).headOption

object EvalCacheEntry:

  case class Id(variant: Variant, smallFen: SmallFen)
