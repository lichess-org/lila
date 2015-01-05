package lila.evaluation
package grouping

import Math.{pow, E, log, sqrt, abs}
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

  def intervalToVariance4(interval: Double): Double = {
    pow(interval / 3, 8) // roughly speaking
  }

  def compareRatings (that: PlayerGroup): Similarity = {
    Similarity(
      bhattCoef(
        this.ratingAvg.perf,
        that.ratingAvg.perf,
        intervalToVariance4(this.ratingAvg.interval),
        intervalToVariance4(that.ratingAvg.interval)
      )
    )
  }

  def compareAges (that: PlayerGroup): Similarity = Similarity(
      (this.ages.tail, that.ages.tail) match {
        case (Nil, Nil) => percentageSimilarity(this.ages.head, that.ages.head)
        case (Nil, a)   => mahaDistance(this.ages.head, NonEmptyList.nel(that.ages.head, a))
        case (a, Nil)   => mahaDistance(that.ages.head, NonEmptyList.nel(this.ages.head, a))
        case (a, b)     => bhattCoef(NonEmptyList.nel(this.ages.head, a), NonEmptyList.nel(that.ages.head, b))
      }
    )

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

    if (playersMatch.matches && gamesMatch.matches) {
      MatchAndSig(true, (playersMatch.significance + gamesMatch.significance) / 2)
    } else {
      MatchAndSig(false, (playersMatch.significance + gamesMatch.significance) / 2)
    }
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

  def compareMoveTimes (that: GameGroup): Similarity = {
    Similarity(bhattCoef(this.moveTimes, that.moveTimes))
  }

  def compareSfAccuracies (that: GameGroup): Similarity = {
    Similarity(bhattCoef(this.sfAccuracies, that.sfAccuracies))
  }

  def compareBlurRates (that: GameGroup): Similarity = {
    (this.blurRates, that.blurRates) match {
      case (Nil, Nil)                   => Similarity(1f) // Both empty
      case (Nil, _ :: _)                => Similarity(0f) // One empty, The other with some
      case (_ :: _, Nil)                => Similarity(0f)
      case (a :: Nil, b :: Nil)         => { // Both have one
        Similarity(percentageSimilarity(a, b))
      } 
      case (a :: Nil, b :: c) => { // One with one element, the other with many
        Similarity(
          mahaDistance(a, NonEmptyList.nel(b, c))
        )
      }
      case (a :: b, c :: Nil) => {
        Similarity(
          mahaDistance(c, NonEmptyList.nel(a, b))
        )
      }
      case (a :: b, c :: d) => { // Both have many
        Similarity(
          bhattCoef(
            NonEmptyList.nel(a, b),
            NonEmptyList.nel(c, d)
          )
        )
      }
    }
  }

  def compareHoldAlerts (that: GameGroup): Similarity = {
    def isQuestionable(holdAlerts: List[HoldAlert]): Boolean = {
      holdAlerts.map(_.turn).toNel match {
        case Some(a)  => ( variance(a, Some(21.5)) < 10 )
        case None     => false
      }
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
  def variance(a: NonEmptyList[Int], optionalAvg: Option[Double] = None): Double = {
    val avg: Double = optionalAvg getOrElse average(a)
    a.map( i => pow(i - avg, 2) ).list.sum / a.length
  }

  def average(a: NonEmptyList[Int]): Double = {
    def average(a: List[Int], sum: Double = 0, depth: Int = 0): Double = {
      a match {
        case List() if (depth != 0) => sum / depth
        case x :: xs => average(xs, sum + x, depth + 1)
      }
    }

    average(a.list)
  }

  def bhattCoef(avgA: Double, avgB: Double, varA: Double, varB: Double): Double = {
    pow(E, (-1.0/4.0) * ( log( (1.0/4.0) * ((varA / varB) + (varB / varA) + 2) ) + pow(avgA - avgB, 2) / ( varA + varB ) ))
  }

  // Bhattacharyya Coefficient
  def bhattCoef(a: NonEmptyList[Int], b: NonEmptyList[Int]): Double = {
    val avgA = average(a)
    val avgB = average(b)

    val varA = pow(variance(a, Some(avgA)), 2)
    val varB = pow(variance(b, Some(avgB)), 2)

    bhattCoef(avgA, avgB, varA, varB)
  }

  // Coefficient of Variance
  def coefVariation(a: NonEmptyList[Int]): Double = sqrt(variance(a)) / average(a)

  // Mahalanobis distance
  def mahaDistance(a: Int, b: NonEmptyList[Int]): Double = abs((a - average(b)) / sqrt(variance(b)))

  def percentageSimilarity(a: Int, b: Int): Double = {
    (a, b) match {
      case (a, b) if (a == b)         => 1
      case (a, b) if (a > 0 && b > 0) => pow(E, -(abs((a - b).toDouble / a) + abs((a - b).toDouble / b)) / 2)
      case _                          => 0
    }
  }

  // all Similarities in the non empty list are similar
  def allSimilar(a: NonEmptyList[Similarity]): Boolean = {
    if (a.list.exists( x => x.isSimilar == false )) false else true
  }

  // Square Sum Distance
  def ssd(a: NonEmptyList[Similarity]): Double = {
    sqrt(a.map(x => pow(x.apply, 2)).list.sum)
  }
}
