package lila.evaluation

import Math.{pow, E, PI, log, sqrt, abs, exp}
import scalaz.NonEmptyList
import chess.{ Color }
import lila.evaluation.GamePool._
import lila.game.{ Pov }
import lila.analyse.{ Accuracy }

case class GameGroupCrossRef(
  _id: String,
  gameId: String,
  color: String, // Side of the game being analysed
  assessment: Int // 1 = Not Cheating, 2 = Unlikely Cheating, 3 = Unknown, 4 = Likely Cheating, 5 = Cheating
  )

case class GameGroupResult(
  _id: String, // sourceGameId + "/" + sourceGameColor
  username: String, // The username of the player being evaluated
  sourceGameId: String, // The game being talked about
  sourceColor: String, // The side of the game being talked about
  targetGameId: String, // The game the source matched against (from crosstable)
  targetColor: String, // The player of the game who was matched against
  positiveMatch: Boolean, // Was the match significant enough to make a hard determination on
  matchPercentage: Int // 0 = Absolutely no match, 100 = Complete match
  )

case class Rating(perf: Int, interval: Int)

case class Similarity(a: Double, threshold: Double = 0.9) {
  def apply: Double = a.min(1).max(0)

  val matches: Boolean = this.apply >= threshold
}
case class MatchAndSig(matches: Boolean, significance: Double)

case class GameGroup(analysed: Analysed, color: Color, assessment: Option[Int] = None) {
  import Statistics._

  def compareMoveTimes (that: GameGroup): Similarity = {
    val thisMt: List[Int] = skip(this.analysed.game.moveTimes.toList, {if (this.color == Color.White) 1 else 0})
    val thatMt: List[Int] = skip(that.analysed.game.moveTimes.toList, {if (that.color == Color.White) 1 else 0})

    listToListSimilarity(thisMt, thatMt, 0.3)
  }

  def compareSfAccuracies (that: GameGroup): Similarity = listToListSimilarity(
    this.analysed.analysis.fold(List(0)){ x => Accuracy.diffsList(Pov(this.analysed.game, this.color), x)},
    that.analysed.analysis.fold(List(0)){ x => Accuracy.diffsList(Pov(that.analysed.game, that.color), x)},
    0.7)

  def compareBlurRates (that: GameGroup): Similarity = pointToPointSimilarity(
    (200 * this.analysed.game.player(this.color).blurs / this.analysed.game.turns).toInt,
    (200 * that.analysed.game.player(that.color).blurs / that.analysed.game.turns).toInt,
    5d
    )

  def compareHoldAlerts (that: GameGroup): Similarity = {
    Similarity(
      if (this.analysed.game.player(this.color).hasSuspiciousHoldAlert == that.analysed.game.player(that.color).hasSuspiciousHoldAlert) 1 else 0,
      0.9
    )
  }

  def similarityTo (that: GameGroup): MatchAndSig = {
    // Calls compare functions to determine how similar `this` and `that` are to each other
    val similarities = NonEmptyList(
      compareMoveTimes(that),
      compareSfAccuracies(that),
      compareBlurRates(that),
      compareHoldAlerts(that)
    )

    MatchAndSig(
      allSimilar(similarities), // Are they all similar?
      ssd(similarities) // How significant is the similarity?
    )
  }
}

object Statistics {
  import Erf._

  def variance[T](a: NonEmptyList[T], optionalAvg: Option[T] = None)(implicit n: Numeric[T]): Double = {
    val avg: Double = optionalAvg.fold(average(a)){n.toDouble}

    a.map( i => pow(n.toDouble(i) - avg, 2)).list.sum / a.length
  }

  def deviation[T](a: NonEmptyList[T], optionalAvg: Option[T] = None)(implicit n: Numeric[T]): Double = sqrt(variance(a, optionalAvg))

  def average[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    def average(a: List[T], sum: T = n.zero, depth: Int = 0): Double = {
      a match {
        case List()  => n.toDouble(sum) / depth
        case x :: xs => average(xs, n.plus(sum, x), depth + 1)
      }
    }
    average(a.list)
  }

  def setToSetSimilarity(avgA: Double, avgB: Double, varA: Double, varB: Double, threshold: Double): Similarity = Similarity(
    pow(E, (-0.25) * ( log( 0.25 * ((varA / varB) + (varB / varA) + 2) ) + pow(avgA - avgB, 2) / ( varA + varB ) )),
    threshold
  )

  // Bhattacharyya Coefficient
  def setToSetSimilarity[T](a: NonEmptyList[T], b: NonEmptyList[T], threshold: Double = 0.9)(implicit n: Numeric[T]): Similarity = {
    val aDouble: NonEmptyList[Double] = a.map(n.toDouble)
    val bDouble: NonEmptyList[Double] = b.map(n.toDouble)

    val avgA = average(a)
    val avgB = average(b)

    val varA = pow(variance(aDouble, Some(avgA)), 2)
    val varB = pow(variance(bDouble, Some(avgB)), 2)

    setToSetSimilarity(avgA, avgB, varA, varB, threshold)
  }

  def listToListSimilarity[T](x: List[T], y: List[T], threshold: Double = 0.9)(implicit n: Numeric[T]): Similarity = {
    (x, y) match {
      case (Nil, Nil)                   => Similarity(1) // Both empty
      case (Nil, _ :: _)                => Similarity(0) // One empty, The other with some
      case (_ :: _, Nil)                => Similarity(0)
      case (a :: Nil, b :: Nil)         => pointToPointSimilarity(a, b, 5d) // Both have one
      case (a :: Nil, b :: c)           => pointToSetSimilarity(a, NonEmptyList.nel(b, c)) // One with one element, the other with many
      case (a :: b, c :: Nil)           => pointToSetSimilarity(c, NonEmptyList.nel(a, b))
      case (a :: b, c :: d)             => setToSetSimilarity(NonEmptyList.nel(a, b), NonEmptyList.nel(c, d), threshold) // Both have many
    }
  }

  def pointToSetSimilarity[T](x: T, set: NonEmptyList[T])(implicit n: Numeric[T]): Similarity = Similarity(
    confInterval(n.toDouble(x), average(set), sqrt(variance(set))),
    0.9
  )

  def pointToPointSimilarity[T](a: T, b: T, variance: Double)(implicit n: Numeric[T]): Similarity = Similarity(
    (a, b) match {
      case (a, b) if (a == b || n.toDouble(n.abs(n.minus(a, b))) < variance) => 1
      case _                                                                 => 0
    }
  )

  // Coefficient of Variance
  def coefVariation(a: NonEmptyList[Int]): Double = sqrt(variance(a)) / average(a)

  def intervalToVariance4(interval: Double): Double = pow(interval / 3, 8) // roughly speaking

  // Accumulative probability function for normal distributions
  def cdf[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double = {
    0.5 * (1 + erf(n.toDouble(n.minus(x, avg)) / (n.toDouble(sd)*sqrt(2))))
  }

  // The probability that you are outside of abs(x-n) from the mean on both sides
  def confInterval[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double = {
    1 - cdf(n.abs(x), avg, sd) + cdf(n.times(n.fromInt(-1), n.abs(x)), avg, sd)
  }

  // all Similarities in the non empty list are similar
  def allSimilar(a: NonEmptyList[Similarity]): Boolean = a.list.forall( _.matches )

  // Square Sum Distance
  def ssd(a: NonEmptyList[Similarity]): Double = sqrt(a.map(x => pow(x.apply, 2)).list.sum / a.size)

  def skip[A](l: List[A], n: Int) = 
    l.zipWithIndex.collect {case (e,i) if ((i+n) % 2) == 0 => e} // (i+1) because zipWithIndex is 0-based
}

object Erf {
  // constants
  val a1: Double =  0.254829592
  val a2: Double = -0.284496736
  val a3: Double =  1.421413741
  val a4: Double = -1.453152027
  val a5: Double =  1.061405429
  val p: Double  =  0.3275911

  def erf(x: Double): Double =  {
    // Save the sign of x
    val sign = if (x < 0) -1 else 1
    val absx = abs(x)

    // A&S formula 7.1.26, rational approximation of error function
    val t = 1.0/(1.0 + p*absx);
    val y = 1.0 - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*exp(-x*x);
    sign*y
  }
}
