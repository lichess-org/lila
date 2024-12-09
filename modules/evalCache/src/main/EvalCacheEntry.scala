package lila.evalCache

import chess.format.{ BinaryFen, Fen }
import chess.Situation
import chess.variant.Variant

import lila.core.chess.MultiPv
import lila.tree.CloudEval

case class EvalCacheEntry(
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[CloudEval]
):
  // finds the best eval with at least multiPv pvs,
  // and truncates its pvs to multiPv.
  // Defaults to lower multiPv if no eval has enough pvs.
  def makeBestMultiPvEval(multiPv: MultiPv): Option[CloudEval] =
    evals
      .find(_.multiPv >= multiPv.atMost(nbMoves))
      .map(_.takePvs(multiPv))
      .orElse:
        evals.sortBy(-_.multiPv.value).headOption

opaque type Id = BinaryFen
object Id extends TotalWrapper[Id, BinaryFen]:
  def apply(sit: Situation): Id = Id(BinaryFen.writeNormalized(sit))

  def from(variant: Variant, fen: Fen.Full): Option[Id] =
    Fen.read(variant, fen).map(Id.apply)
