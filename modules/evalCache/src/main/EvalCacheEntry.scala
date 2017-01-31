package lila.evalCache

import chess.format.{ Forsyth, FEN, Uci }
import org.joda.time.DateTime
import scalaz.NonEmptyList

import lila.tree.Eval.{ Score }

case class EvalCacheEntry(
    _id: EvalCacheEntry.Id,
    evals: List[EvalCacheEntry.TrustedEval]) {

  import EvalCacheEntry._

  def id = _id

  def fen = _id.fen
  def multiPv = _id.multiPv

  def bestEval: Option[Eval] = evals.headOption.map(_.eval)

  def add(eval: Eval) = copy(evals = TrustedEval(Trust(1), eval) :: evals)
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
      engine: String,
      by: lila.user.User.ID,
      date: DateTime) {

    def bestPv: Pv = pvs.head

    def bestMove: Uci = bestPv.moves.value.head

    def isValid = pvs.list.forall(_.isValid) && {
      pvs.list.forall(_.score.mateFound) || (nodes >= MIN_NODES && depth >= MIN_DEPTH)
    }

    def truncatePvs = copy(pvs = pvs.map(_.truncate))
  }

  case class Pv(score: Score, moves: Moves) {

    def isValid = score.mateFound || moves.value.size > MIN_PV_SIZE

    def truncate = copy(moves = moves.truncate)
  }

  case class Moves(value: NonEmptyList[Uci]) extends AnyVal {

    def truncate = copy(value = NonEmptyList.nel(value.head, value.tail.take(MAX_PV_SIZE - 1)))
  }

  case class Trust(value: Double) extends AnyVal

  final class MultiPv private (val value: Int) extends AnyVal

  object MultiPv {
    private val range = 1 to MAX_MULTI_PV
    def apply(value: Int): Option[MultiPv] = range.contains(value) option { new MultiPv(value) }
  }

  case class Id(fen: SmallFen, multiPv: MultiPv)

  object Id {
    def apply(fenStr: String, multiPvInt: Int): Option[Id] = for {
      multiPv <- MultiPv(multiPvInt)
      fen <- Forsyth.<<(fenStr).exists(_ playable false) option FEN(fenStr)
    } yield Id(SmallFen from fen, multiPv)
  }

  case class SmallFen(value: String) extends AnyVal

  object SmallFen {
    def from(fen: FEN): SmallFen = SmallFen {
      fen.value.replace("/", "").split(' ').take(4).mkString(" ")
    }
  }

  case class Input(id: Id, eval: Eval) {
    def entry(trust: Trust) = EvalCacheEntry(id, List(TrustedEval(trust, eval)))
  }

  object Input {
    case class Candidate(fen: String, eval: Eval) {
      def input = Id(fen, eval.pvs.size) ifTrue eval.isValid map { id =>
        Input(id, eval.truncatePvs)
      }
    }
  }
}
