package lila.evalCache

import chess.format.{ Forsyth, FEN, Uci }
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.tree.Eval.{ Score }

case class EvalCacheEntry(
    _id: EvalCacheEntry.SmallFen,
    evals: List[EvalCacheEntry.TrustedEval],
    accessedAt: DateTime) {

  import EvalCacheEntry._

  def fen = _id

  def add(trustedEval: TrustedEval) = copy(
    evals = EvalCacheSelector(trustedEval :: evals),
    accessedAt = DateTime.now)

  def bestMultiPvEval(multiPv: Int): Option[Eval] =
    evals.map(_.eval).find(_.multiPv >= multiPv)
}

object EvalCacheEntry {

  val MIN_NODES = 5 * math.pow(10, 6)
  val MIN_DEPTH = 20
  val MIN_PV_SIZE = 6
  val MAX_PV_SIZE = 8
  val MAX_MULTI_PV = 5

  case class TrustedEval(trust: Trust, eval: Eval)

  case class Eval(
      pvs: NonEmptyList[Pv],
      nodes: Int,
      depth: Int,
      by: lila.user.User.ID,
      date: DateTime) {

    def multiPv = pvs.size

    def bestPv: Pv = pvs.head

    def bestMove: Uci = bestPv.moves.value.head

    def isValid = pvs.list.forall(_.isValid) && {
      pvs.list.forall(_.score.mateFound) || (nodes >= MIN_NODES && depth >= MIN_DEPTH)
    }

    def truncatePvs = copy(pvs = pvs.map(_.truncate))

    def depthAboveMin = (depth - MIN_DEPTH) atLeast 0
  }

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
    def trusted(fen: FEN) = new SmallFen(fen.value)
    def validate(fen: FEN): Option[SmallFen] =
      Forsyth.<<(fen.value).exists(_ playable false) option {
        new SmallFen(fen.value.replace("/", "").split(' ').take(4).mkString(" "))
      }
  }

  case class Input(fen: SmallFen, eval: Eval) {
    def trusted(trust: Trust) = TrustedEval(trust, eval)
    def entry(trust: Trust) = EvalCacheEntry(fen, List(trusted(trust)), DateTime.now)
  }

  object Input {
    case class Candidate(fen: String, eval: Eval) {
      def input = SmallFen.validate(FEN(fen)) ifTrue eval.isValid map {
        Input(_, eval.truncatePvs)
      }
    }
  }
}
