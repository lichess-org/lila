package lila.analyse

import lila.game.Game.SideAndStart
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

// Quality of a move, based on previous and next WinPercent
case class AccuracyPercent private (value: Double) extends AnyVal {
  def toInt = value.toInt
}

object AccuracyPercent {

  import WinPercent.BeforeAfter

  val perfect = AccuracyPercent(100)

  /*
from scipy.optimize import curve_fit
import numpy as np

def model_func(x, a, k, b):
    return a * np.exp(-k*x) + b

# sample data
x = np.array([  0,  5, 10, 20, 40, 60, 80, 90, 100])
y = np.array([100, 75, 60, 42, 20,  5,  0,  0,   0])

# curve fit
p0 = (1.,1.e-5,1.) # starting search koefs
opt, pcov = curve_fit(model_func, x, y, p0)
a, k, b = opt
print(f"{a} * exp(-{k} * x) + {b}")
   */
  def fromWinPercents(before: WinPercent, after: WinPercent): AccuracyPercent = AccuracyPercent {
    if (after.value >= before.value) 100d
    else {
      val diff = before.value - after.value
      // 105.571942 * Math.exp(-0.037088731 * diff) - 4.08425894
      98.70882650493327 * Math.exp(-0.04227403940198299 * diff) + -1.852425254357673
    } atMost 100 atLeast 0
  }

  def fromWinPercents(both: BeforeAfter): AccuracyPercent =
    fromWinPercents(both.before, both.after)

  def fromEvalsAndPov(pov: SideAndStart, evals: List[Eval]): List[AccuracyPercent] = {
    val subjectiveEvals = pov.color.fold(evals, evals.map(_.invert))
    val alignedEvals = if (pov.color == pov.startColor) Eval.initial :: subjectiveEvals else subjectiveEvals
    alignedEvals
      .grouped(2)
      .collect { case List(e1, e2) =>
        for {
          before <- WinPercent.fromEval(e1)
          after  <- WinPercent.fromEval(e2)
        } yield AccuracyPercent.fromWinPercents(before, after)
      }
      .flatten
      .toList
  }

  def fromAnalysisAndPov(pov: SideAndStart, analysis: Analysis): List[AccuracyPercent] =
    fromEvalsAndPov(pov, analysis.infos.map(_.eval))
}
