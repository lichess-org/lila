package lila.evalCache

import chess.format.{ Forsyth, FEN, Uci }
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.tree.Eval.{ Score }

case class EvalCacheEntry(
    _id: EvalCacheEntry.SmallFen,
    nbMoves: Int, // multipv cannot be greater than number of legal moves
    evals: List[EvalCacheEntry.Eval],
    usedAt: DateTime) {

  import EvalCacheEntry._

  def fen = _id

  def add(eval: Eval) = copy(
    evals = EvalCacheSelector(eval :: evals),
    usedAt = DateTime.now)

  // finds the best eval with at least multiPv pvs,
  // and truncates its pvs to multiPv
  def makeBestMultiPvEval(multiPv: Int): Option[Eval] =
    evals
      .find(_.multiPv >= multiPv.atMost(nbMoves))
      .map(_ takePvs multiPv)

  def similarTo(other: EvalCacheEntry) =
    fen == other.fen && evals == other.evals
}

object EvalCacheEntry {

  val MIN_KNODES = 3000
  val MIN_DEPTH = 20
  val MIN_PV_SIZE = 6
  val MAX_PV_SIZE = 10
  val MAX_MULTI_PV = 5

  case class Eval(
      pvs: NonEmptyList[Pv],
      knodes: Knodes,
      depth: Int,
      by: lila.user.User.ID,
      trust: Trust) {

    def multiPv = pvs.size

    def bestPv: Pv = pvs.head

    def bestMove: Uci = bestPv.moves.value.head

    def isValid = pvs.list.forall(_.isValid) && {
      pvs.list.forall(_.score.mateFound) || (knodes.value >= MIN_KNODES || depth >= MIN_DEPTH)
    }

    def truncatePvs = copy(pvs = pvs.map(_.truncate))

    def takePvs(multiPv: Int) = copy(
      pvs = NonEmptyList.nel(pvs.head, pvs.tail.take(multiPv - 1)))

    def depthAboveMin = (depth - MIN_DEPTH) atLeast 0
  }

  case class Knodes(value: Int) extends AnyVal

  case class Pv(score: Score, moves: Moves) {

    def isValid = score.mateFound || moves.value.size > MIN_PV_SIZE

    def truncate = copy(moves = moves.truncate)
  }

  case class Moves(value: NonEmptyList[Uci]) extends AnyVal {

    def truncate = copy(value = NonEmptyList.nel(value.head, value.tail.take(MAX_PV_SIZE - 1)))
  }

  case class Trust(value: Double) extends AnyVal {
    def isTooLow = value <= 0
  }

  case class TrustedUser(trust: Trust, user: lila.user.User)

  final class SmallFen private (val value: String) extends AnyVal

  object SmallFen {
    private[evalCache] def raw(str: String) = new SmallFen(str)
    def make(fen: FEN) = new SmallFen(
      fen.value.split(' ').take(4).mkString("").filter { c =>
        c != '/' && c != '-' && c != 'w'
      }
    )
    def validate(fen: FEN): Option[SmallFen] =
      Forsyth.<<(fen.value).exists(_ playable false) option make(fen)
  }

  case class Input(fen: FEN, smallFen: SmallFen, eval: Eval)

  object Input {
    case class Candidate(fen: String, eval: Eval) {
      def input = SmallFen.validate(FEN(fen)) ifTrue eval.isValid map {
        Input(FEN(fen), _, eval.truncatePvs)
      }
    }
  }
}
