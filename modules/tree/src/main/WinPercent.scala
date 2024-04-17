package lila.tree

import lila.core.data.Percent

// How likely one is to win a position, based on subjective Stockfish centipawns
opaque type WinPercent = Double
object WinPercent extends OpaqueDouble[WinPercent]:

  // given lila.db.NoDbHandler[WinPercent] with {}
  given Percent[WinPercent] = Percent.of(WinPercent)

  extension (a: WinPercent) def toInt = Percent.toInt(a)

  def fromEval(eval: Eval): Option[WinPercent] =
    eval.cp.map(fromCentiPawns).orElse(eval.mate.map(fromMate))

  def fromMate(mate: Eval.Mate) = fromCentiPawns(Eval.Cp.ceilingWithSignum(mate.signum))

  // [0, 100]
  def fromCentiPawns(cp: Eval.Cp) = WinPercent {
    50 + 50 * winningChances(cp.ceiled)
  }

  inline def fromPercent(int: Int) = WinPercent(int.toDouble)

  // [-1, +1]
  private[tree] def winningChances(cp: Eval.Cp) = {
    val MULTIPLIER = -0.00368208 // https://github.com/lichess-org/lila/pull/11148
    2 / (1 + Math.exp(MULTIPLIER * cp.value)) - 1
  }.atLeast(-1).atMost(+1)
