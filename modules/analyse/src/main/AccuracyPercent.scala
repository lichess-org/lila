package lila.analyse

import lila.common.Maths
import lila.game.Game.SideAndStart
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

// Quality of a move, based on previous and next WinPercent
case class AccuracyPercent private (value: Double) extends AnyVal with Percent

object AccuracyPercent {

  import WinPercent.BeforeAfter

  def fromPercent(int: Int) = AccuracyPercent(int.toDouble)

  val perfect = fromPercent(100)

  implicit val ordering = Ordering.by[AccuracyPercent, Double](_.value)

  /*
from scipy.optimize import curve_fit
import numpy as np

def model_func(x, a, k, b):
    return a * np.exp(-k*x) + b

# sample data
xs      = np.array([    0,  5, 10, 20, 40, 60,    80, 90, 100])
ys      = np.array([  100, 75, 60, 42, 20,  5,     0,  0,   0])
sigma   = np.array([0.005,  1,  1,  1,  1,  1, 0.005,  1,   1]) # error stdev

opt, pcov = curve_fit(model_func, xs, ys, None, sigma)
a, k, b = opt
print(f"{a} * exp(-{k} * x) + {b}")
for x in xs:
    print(f"f({x}) = {model_func(x, a, k, b)}");
   */
  def fromWinPercentDelta(delta: WinPercent.Delta): AccuracyPercent = AccuracyPercent {
    if (delta.value <= 0) 100d
    else
      {
        103.1668100711649 * Math.exp(-0.04354415386753951 * delta.value) + -3.166924740191411;
      } atMost 100 atLeast 0
  }

  // returns None if one or more evals have no score (incomplete analysis)
  def winPercents(pov: SideAndStart, evals: List[Eval]): Option[List[WinPercent]] = {
    val subjectiveEvals = pov.color.fold(evals, evals.map(_.invert))
    val alignedEvals = if (pov.color == pov.startColor) Eval.initial :: subjectiveEvals else subjectiveEvals
    alignedEvals.flatMap(WinPercent.fromEval).some.filter(_.sizeCompare(evals) == 0)
  }

  def fromEvalsAndPov(pov: SideAndStart, evals: List[Eval]): Option[List[AccuracyPercent]] =
    winPercents(pov, evals) map WinPercent.deltas map {
      _ map AccuracyPercent.fromWinPercentDelta
    }

  def fromAnalysisAndPov(pov: SideAndStart, analysis: Analysis): Option[List[AccuracyPercent]] =
    fromEvalsAndPov(pov, analysis.infos.map(_.eval))

  def gameAccuracy(pov: SideAndStart, analysis: Analysis): Option[AccuracyPercent] = for {
    wins            <- winPercents(pov, analysis.infos.map(_.eval))
    firstWinPercent <- wins.headOption
    accuracies = WinPercent deltas wins map AccuracyPercent.fromWinPercentDelta
    windowSize = 6
    windows    = (List.fill(windowSize - 1)(firstWinPercent) ::: wins).map(_.value).sliding(windowSize).toList
    weights    = windows map { xs => ~Maths.standardDeviation(xs) }
  } yield ???
}
