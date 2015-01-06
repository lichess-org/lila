package lila.evaluation
package grouping

import Math.{pow, E, PI, log, sqrt, abs, exp}
import scalaz.NonEmptyList

case class HoldAlert(average: Int, deviation: Int, turn: Int)
case class Rating(perf: Int, interval: Int)

case class Similarity(a: Double) {
  def apply: Double = a.min(1).max(0)

  val isSimilar: Boolean = this.apply > 0.9
}
case class MatchAndSig(matches: Boolean, significance: Double)

case class PlayerGroup (
    games: GameGroup,
    ratings: NonEmptyList[Rating],
    ages: NonEmptyList[Int]
  ) {
  import Statistics._

  val ratingAvg = averageRating(ratings)

  def averageRating(a: NonEmptyList[Rating]): Rating = {
    def averageRating(a: List[Rating], sum: Rating = Rating(0, 0), depth: Int = 0): Rating = {
      a match {
        case List()  => Rating(sum.perf / depth, sum.interval / depth)
        case x :: xs => averageRating(xs, Rating(sum.perf + x.perf, sum.interval + x.interval), depth + 1)
      }
    }
    
    averageRating(a.list)
  }

  def compareRatings (that: PlayerGroup): Similarity = setToSetSimilarity(
    this.ratingAvg.perf,
    that.ratingAvg.perf,
    intervalToVariance4(this.ratingAvg.interval),
    intervalToVariance4(that.ratingAvg.interval)
  )

  def compareAges (that: PlayerGroup): Similarity = (this.ages.tail, that.ages.tail) match {
    case (Nil, Nil) => pointToPointSimilarity(this.ages.head, that.ages.head)
    case (Nil, a)   => pointToSetSimilarity(this.ages.head, NonEmptyList.nel(that.ages.head, a))
    case (a, Nil)   => pointToSetSimilarity(that.ages.head, NonEmptyList.nel(this.ages.head, a))
    case (a, b)     => setToSetSimilarity(NonEmptyList.nel(this.ages.head, a), NonEmptyList.nel(that.ages.head, b))
  }

  def similarityTo (that: PlayerGroup): MatchAndSig = {
    // Calls compare functions to determine how similar `this` and `that` are to each other
    val similarities = NonEmptyList(
      compareRatings(that),
      compareAges(that)
    )
    val playersMatch = MatchAndSig(
      allSimilar(similarities), // Are they all similar?
      ssd(similarities) // How significant is the similarity?
    )
    val gamesMatch = this.games.similarityTo(that.games)
    
    MatchAndSig(
      (playersMatch.matches && gamesMatch.matches),
      (playersMatch.significance + gamesMatch.significance) / 2
    )
  }
}

case class GameGroup (
  moveTimes: NonEmptyList[Int],
  sfAccuracies: NonEmptyList[Int],
  blurRates: List[Int],
  holdAlerts: List[HoldAlert]
  ) {
  import Statistics._

  val holdAlertsAvg = averageHoldAlerts(holdAlerts)

  def averageHoldAlerts(a: List[HoldAlert], sum: HoldAlert = HoldAlert(0, 0, 0), depth: Int = 0): Option[HoldAlert] = {
    a match {
      case List() if (depth != 0) => Some(HoldAlert(sum.average / depth, sum.deviation / depth, sum.turn / depth))
      case x :: xs => averageHoldAlerts(xs, HoldAlert(sum.average + x.average, sum.deviation + x.deviation, sum.turn + x.turn), depth + 1)
      case Nil => None
    }
  }

  def compareMoveTimes (that: GameGroup): Similarity = pointToPointSimilarity(coefVariation(this.moveTimes), coefVariation(that.moveTimes))

  def compareSfAccuracies (that: GameGroup): Similarity = setToSetSimilarity(this.sfAccuracies, that.sfAccuracies)

  def compareBlurRates (that: GameGroup): Similarity = {
    (this.blurRates, that.blurRates) match {
      case (Nil, Nil)                   => Similarity(1) // Both empty
      case (Nil, _ :: _)                => Similarity(0) // One empty, The other with some
      case (_ :: _, Nil)                => Similarity(0)
      case (a :: Nil, b :: Nil)         => pointToPointSimilarity(a, b) // Both have one
      case (a :: Nil, b :: c)           => pointToSetSimilarity(a, NonEmptyList.nel(b, c)) // One with one element, the other with many
      case (a :: b, c :: Nil)           => pointToSetSimilarity(c, NonEmptyList.nel(a, b))
      case (a :: b, c :: d)             => setToSetSimilarity(NonEmptyList.nel(a, b), NonEmptyList.nel(c, d)) // Both have many    
    }
  }

  def compareHoldAlerts (that: GameGroup): Similarity = {
    def isQuestionable(holdAlerts: List[HoldAlert]): Boolean = {
      holdAlerts.map(_.turn).toNel.fold(false){variance(_, Some(21)) < 10}
    }
    Similarity(
      if (isQuestionable(this.holdAlerts) == isQuestionable(that.holdAlerts)) 1
      else 0
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

  def average[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    def average(a: List[T], sum: T = n.zero, depth: Int = 0): Double = {
      a match {
        case List()  => n.toDouble(sum) / depth
        case x :: xs => average(xs, n.plus(sum, x), depth + 1)
      }
    }
    average(a.list)
  }

  def setToSetSimilarity(avgA: Double, avgB: Double, varA: Double, varB: Double): Similarity = Similarity(
    pow(E, (-0.25) * ( log( 0.25 * ((varA / varB) + (varB / varA) + 2) ) + pow(avgA - avgB, 2) / ( varA + varB ) ))
  )

  // Bhattacharyya Coefficient
  def setToSetSimilarity[T](a: NonEmptyList[T], b: NonEmptyList[T])(implicit n: Numeric[T]): Similarity = {
    val aDouble: NonEmptyList[Double] = a.map(n.toDouble)
    val bDouble: NonEmptyList[Double] = b.map(n.toDouble)

    val avgA = average(a)
    val avgB = average(b)

    val varA = pow(variance(aDouble, Some(avgA)), 2)
    val varB = pow(variance(bDouble, Some(avgB)), 2)

    setToSetSimilarity(avgA, avgB, varA, varB)
  }

  def pointToSetSimilarity[T](x: T, set: NonEmptyList[T])(implicit n: Numeric[T]): Similarity = Similarity(
    confInterval(n.toDouble(x), average(set), sqrt(variance(set)))
  )

  def pointToPointSimilarity[T](a: T, b: T)(implicit n: Numeric[T]): Similarity = Similarity(
    (a, b) match {
      case (a, b) if (a == b)                                               => 1
      case (a, b) if (n.compare(a, n.zero) > 0 && n.compare(b, n.zero) > 0) => pow(E, -(abs(n.toDouble(n.minus(a, b)) / n.toDouble(a)) + abs(n.toDouble(n.minus(a, b)) / n.toDouble(b))) / 2)
      case _                                                                => 0
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
  def allSimilar(a: NonEmptyList[Similarity]): Boolean = a.list.forall( _.isSimilar )

  // Square Sum Distance
  def ssd(a: NonEmptyList[Similarity]): Double = sqrt(a.map(x => pow(x.apply, 2)).list.sum) / a.size
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
