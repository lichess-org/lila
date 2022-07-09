package lila.analyse

import lila.tree.Eval

// How likely one is to win a position, based on subjective Stockfish centipawns
case class WinPercent(value: Double) extends AnyVal with Percent

object WinPercent {

  def fromEval(eval: Eval): Option[WinPercent] =
    eval.cp.map(fromCentiPawns) orElse eval.mate.map(fromMate)

  def fromMate(mate: Eval.Mate) = fromCentiPawns(Eval.Cp(Eval.Cp.CEILING * mate.signum))

  // [0, 100]
  def fromCentiPawns(cp: Eval.Cp) = WinPercent {
    50 + 50 * winningChances(cp.ceiled)
  }

  def fromPercent(int: Int) = WinPercent(int.toDouble)

  // [-1, +1]
  private[analyse] def winningChances(cp: Eval.Cp) = {
    val MULTIPLIER = -0.00368208 // https://github.com/lichess-org/lila/pull/11148
    2 / (1 + Math.exp(MULTIPLIER * cp.value)) - 1
  } atLeast -1 atMost +1

  case class BeforeAfter(before: WinPercent, after: WinPercent)
}
