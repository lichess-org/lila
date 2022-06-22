package lila.analyse

import lila.tree.Eval

// How likely one is to win a position, based on subjective Stockfish centipawns
case class WinPercent(value: Double) extends AnyVal {
  def toInt = value.toInt
}

object WinPercent {

  def fromEval(eval: Eval): Option[WinPercent] =
    eval.cp.map(fromCentiPawns) orElse eval.mate.map(fromMate)

  def fromMate(mate: Eval.Mate) = fromCentiPawns(Eval.Cp(Eval.Cp.CEILING * mate.signum))

  def fromCentiPawns(cp: Eval.Cp) = WinPercent {
    50 + 50 * winningChances(cp)
  }

  case class BeforeAfter(before: WinPercent, after: WinPercent)

  // [-1, +1]
  private[analyse] def winningChances(cp: Eval.Cp) = {
    2 / (1 + Math.exp(-0.004 * cp.value)) - 1
  } atLeast -1 atMost +1
}
