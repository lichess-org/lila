package lila.analyse

import chess.Color

import lila.common.Maths
import lila.game.Game
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

// Quality of a move, based on previous and next WinPercent
case class AccuracyPercent private (value: Double) extends AnyVal with Percent {
  def *(weight: Double) = copy(value * weight)
}

object AccuracyPercent {

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
  def fromWinPercents(before: WinPercent, after: WinPercent): AccuracyPercent = AccuracyPercent {
    if (after.value >= before.value) 100d
    else
      {
        val winDiff = before.value - after.value
        103.1668100711649 * Math.exp(-0.04354415386753951 * winDiff) + -3.166924740191411;
      } atMost 100 atLeast 0
  }

  // returns None if one or more evals have no score (incomplete analysis)
  // def winPercents(pov: Game.SideAndStart, evals: List[Eval]): Option[List[WinPercent]] = {
  //   val subjectiveEvals = pov.color.fold(evals, evals.map(_.invert))
  //   val alignedEvals = if (pov.color == pov.startColor) Eval.initial :: subjectiveEvals else subjectiveEvals
  //   alignedEvals.flatMap(WinPercent.fromEval).some.filter(_.sizeCompare(evals) == 0)
  // }

  def fromEvalsAndPov(pov: Game.SideAndStart, evals: List[Eval]): List[AccuracyPercent] = {
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

  def fromAnalysisAndPov(pov: Game.SideAndStart, analysis: Analysis): List[AccuracyPercent] =
    fromEvalsAndPov(pov, analysis.infos.map(_.eval))

  def gameAccuracy(startColor: Color, analysis: Analysis): Option[Color.Map[AccuracyPercent]] = {
    val evalsWithInitial = Eval.initial :: analysis.infos.map(_.eval)
    val allWinPercents   = evalsWithInitial flatMap WinPercent.fromEval
    allWinPercents.headOption map { firstWinPercent =>
      val windowSize = 6
      val windows = {
        List.fill(windowSize - 1)(firstWinPercent) ::: allWinPercents
      }.map(_.value).sliding(windowSize).toList
      val weights = windows map { xs => ~Maths.standardDeviation(xs) }
      val weightedAccuracies: Iterable[((Double, Double), Color)] = allWinPercents
        .sliding(2)
        .zip(weights)
        .zipWithIndex
        .collect { case ((List(prev, next), weight), i) =>
          val color    = Color.fromWhite((i % 2 == 0) == startColor.white)
          val accuracy = AccuracyPercent.fromWinPercents(color.fold(prev, next), color.fold(next, prev)).value
          ((accuracy, weight), color)
        }
        .to(Iterable)

      evalsWithInitial.drop(1).map(_.forceAsCp.??(_.value)).zip(weightedAccuracies) foreach {
        case (eval, ((acc, weight), color)) =>
          println(s"$eval $color $weight $acc")
      }

      Color.Map.apply { color =>
        new AccuracyPercent(~Maths.weightedMean {
          weightedAccuracies collect {
            case (weightedAccuracy, c) if c == color => weightedAccuracy
          }
        })
      }
    }
  }
}
