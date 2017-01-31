package lila.evalCache

import chess.format.{ FEN, Uci }
import org.joda.time.DateTime

import lila.tree.Eval.{ Score }

case class EvalCacheEntry(
    _id: FEN,
    evals: List[EvalCacheEntry.Eval]) {

  import EvalCacheEntry._

  def add(eval: Eval) = copy(evals = eval :: evals)
}

object EvalCacheEntry {

  case class Trust(value: Double) extends AnyVal

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
}
