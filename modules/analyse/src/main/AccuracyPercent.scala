package lila.analyse

import chess.{ ByColor, Color }
import chess.eval.WinPercent
import chess.eval.Eval.Cp
import scalalib.Maths
import scalalib.model.Percent

import lila.core.game.SideAndStart
import lila.tree.{ Analysis, Eval }

// Quality of a move, based on previous and next WinPercent
opaque type AccuracyPercent = Double
object AccuracyPercent extends OpaqueDouble[AccuracyPercent]:

  given lila.db.NoDbHandler[AccuracyPercent] with {}
  given Percent[AccuracyPercent] = Percent.of(AccuracyPercent)

  extension (a: AccuracyPercent)
    inline def +(inline d: Double) = apply(a.value + d)
    inline def *(inline weight: Double) = apply(a.value * weight)
    inline def mean(inline other: AccuracyPercent) = apply((a.value + other.value) / 2)
    inline def toInt = Percent.toInt(a)

  inline def fromPercent(int: Int) = AccuracyPercent(int.toDouble)

  val perfect = fromPercent(100)

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
  def fromWinPercents(before: WinPercent, after: WinPercent): AccuracyPercent = AccuracyPercent:
    if after.value >= before.value then 100d
    else
      {
        val winDiff = before.value - after.value
        val raw = 103.1668100711649 * Math.exp(-0.04354415386753951 * winDiff) + -3.166924740191411
        raw + 1 // uncertainty bonus (due to imperfect analysis)
      }.atMost(100).atLeast(0)

  def fromEvalsAndPov(pov: SideAndStart, evals: List[Eval]): List[AccuracyPercent] =
    val subjectiveEvals = pov.color.fold(evals, evals.map(_.invert))
    val alignedEvals =
      if pov.color == pov.startColor
      then lila.tree.evals.initial :: subjectiveEvals
      else subjectiveEvals
    alignedEvals
      .grouped(2)
      .collect:
        case List(e1, e2) =>
          for
            before <- e1.score.map(WinPercent.fromScore)
            after <- e2.score.map(WinPercent.fromScore)
          yield AccuracyPercent.fromWinPercents(before, after)
      .flatten
      .toList

  def fromAnalysisAndPov(pov: SideAndStart, analysis: Analysis): List[AccuracyPercent] =
    fromEvalsAndPov(pov, analysis.infos.map(_.eval))

  def gameAccuracy(startColor: Color, analysis: Analysis): Option[ByColor[AccuracyPercent]] =
    gameAccuracy(startColor, analysis.infos.map(_.eval.forceAsCp))

  // a mean of volatility-weighted mean and harmonic mean
  def gameAccuracy(startColor: Color, cps: List[Option[Cp]]): Option[ByColor[AccuracyPercent]] =
    val allWinPercents = (Some(Cp.initial) :: cps).map(_.map(WinPercent.fromCentiPawns))
    val windowSize = (cps.size / 10).squeeze(2, 8)

    val windows = List.fill(windowSize.atMost(allWinPercents.size) - 2)(allWinPercents.take(windowSize))
      ::: allWinPercents.sliding(windowSize).toList
    val weights = windows.map:
      _.sequence.map(wp => Maths.standardDeviation(WinPercent.raw(wp)).so(_.squeeze(0.5, 12)))
    val weightedAccuracies: Iterable[(Option[PairOf[AccuracyPercent]], Color)] = allWinPercents
      .sliding(2)
      .zip(weights)
      .zipWithIndex
      .collect { case ((List(prev, next), weight), i) =>
        val color = Color.fromWhite((i % 2 == 0) == startColor.white)
        val weighted = for
          p <- prev
          n <- next
          w <- weight
          accuracy = AccuracyPercent.fromWinPercents(color.fold(p, n), color.fold(n, p))
        yield (accuracy, w)
        (weighted, color)
      }
      .to(Iterable)

    def colorAccuracy(color: Color) = for
      weighted <- Maths.weightedMean:
        weightedAccuracies.collect:
          case (Some(weightedAccuracy), c) if c == color => weightedAccuracy
      harmonic <- Maths.harmonicMean:
        weightedAccuracies.collect:
          case (Some((accuracy, _)), c) if c == color => accuracy
    yield AccuracyPercent((weighted + harmonic) / 2)

    ByColor(colorAccuracy)
