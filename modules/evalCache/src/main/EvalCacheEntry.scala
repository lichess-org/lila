package lila.evalCache

import chess.format.{ Forsyth, FEN, Uci }
import org.joda.time.DateTime

import lila.tree.Eval.{ Score }

case class EvalCacheEntry(
    _id: EvalCacheEntry.Id,
    evals: List[EvalCacheEntry.Eval]) {

  import EvalCacheEntry._

  def id = _id

  def fen = _id.fen
  def multiPv = _id.multiPv

  def bestEval: Option[Eval] = evals.headOption

  def add(eval: Eval) = copy(evals = eval :: evals)
}

object EvalCacheEntry {

  val MIN_NODES = 5 * math.pow(10, 6)
  val MIN_DEPTH = 20
  val MIN_PV_SIZE = 6
  val MAX_PV_SIZE = 8
  val MAX_MULTI_PV = 5

  case class Eval(
      score: Score,
      pv: Pv,
      nodes: Int,
      depth: Int,
      engine: String,
      by: lila.user.User.ID,
      trust: Trust,
      date: DateTime) {

    def bestMove: Option[Uci] = pv.value.headOption

    def isValid = score.mateFound || {
      nodes >= MIN_NODES && depth >= MIN_DEPTH && pv.value.size > MIN_PV_SIZE
    }
  }

  case class Pv(value: List[Uci]) extends AnyVal {

    def truncate = Pv(value take MAX_PV_SIZE)
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
    def entry = EvalCacheEntry(id, List(eval))
  }

  object Input {
    case class Candidate(fen: String, multiPv: Int, eval: Eval) {
      def input = Id(fen, multiPv) ifTrue eval.isValid map { id =>
        Input(id, eval.copy(pv = eval.pv.truncate))
      }
    }
  }
}
