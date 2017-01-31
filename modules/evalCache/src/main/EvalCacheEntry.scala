package lila.evalCache

import chess.format.{ FEN, Uci }
import org.joda.time.DateTime

import lila.tree.Eval.{ Score }

case class EvalCacheEntry(
    _id: EvalCacheEntry.Id,
    evals: List[EvalCacheEntry.Eval]) {

  import EvalCacheEntry._

  def id = _id

  def fen = _id.fen
  def multiPv = _id.multiPv

  def add(eval: Eval) = copy(evals = eval :: evals)
}

object EvalCacheEntry {

  case class Eval(
      score: Score,
      pv: List[Uci],
      nodes: Int,
      depth: Int,
      engine: String,
      by: lila.user.User.ID,
      trust: Trust,
      date: DateTime) {
  }

  case class Trust(value: Double) extends AnyVal
  case class MultiPv(value: Int) extends AnyVal
  case class Id(fen: SmallFen, multiPv: MultiPv)

  case class SmallFen(value: String) extends AnyVal

  object SmallFen {
    def from(fen: FEN): SmallFen = SmallFen {
      fen.value.replace("/", "").split(' ').take(4).mkString(" ")
    }
  }

  case class Input(fen: SmallFen, multiPv: MultiPv, eval: Eval) {
    def id = Id(fen, multiPv)
    def entry = EvalCacheEntry(id, List(eval))
  }
}
