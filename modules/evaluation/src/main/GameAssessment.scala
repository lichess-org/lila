package lila.evaluation

import chess.{ Color }
import lila.game.{ Pov, Game }
import lila.analyse.{ Accuracy, Analysis }
import Math.{ pow, abs, sqrt, E, exp }
import org.joda.time.DateTime
import scalaz.NonEmptyList

case class PlayerAssessment(
  _id: String,
  gameId: String,
  userId: String,
  white: Boolean,
  assessment: Int, // 1 = Not Cheating, 2 = Unlikely Cheating, 3 = Unknown, 4 = Likely Cheating, 5 = Cheating
  date: DateTime,
  // meta
  sfAvg: Int,
  sfSd: Int,
  mtAvg: Int,
  mtSd: Int,
  blurs: Int,
  hold: Boolean
  ) {
  val color = Color.apply(white)
}

case class PlayerAggregateAssessment(playerAssessments: List[PlayerAssessment]) {
  import Statistics._

  def countAssessmentValue(assessment: Int) = listSum(playerAssessments map {
    case a if (a.assessment == assessment) => 1
    case _ => 0
  })

  val cheatingSum = countAssessmentValue(5)
  val likelyCheatingSum = countAssessmentValue(4)

  val markPri = countAssessmentValue(5) >= 2
  val markSec = countAssessmentValue(5) + countAssessmentValue(4) >= 4

  val reportPri = countAssessmentValue(5) >= 1
  val reportSec = countAssessmentValue(5) + countAssessmentValue(4) >= 2

}

case class GameAssessments(
  white: Option[PlayerAssessment],
  black: Option[PlayerAssessment]) {
  def color(c: Color) = c match {
    case Color.White => white
    case _ => black
  }
  }

case class Analysed(game: Game, analysis: Analysis)

case class Assessible(analysed: Analysed) {
  import Statistics._

  def moveTimes(color: Color): List[Int] =
    skip(this.analysed.game.moveTimes.toList, {if (color == Color.White) 0 else 1})

  def suspiciousErrorRate(color: Color): Boolean =
    listAverage(Accuracy.diffsList(Pov(this.analysed.game, color), this.analysed.analysis)) < 15

  def alwaysHasAdvantage(color: Color): Boolean = {
    def chartFromDifs(difs: List[(Int, Int)], sum: Int = 0): List[Int] =
      difs match {
        case Nil      => Nil
        case a :: Nil => List((sum + a._1), (sum + a._1 + a._2))
        case a :: b   => List((sum + a._1), (sum + a._1 + a._2)) ::: chartFromDifs(b, sum + a._1 + a._2)
      }

    !chartFromDifs(Accuracy.diffsList(Pov(this.analysed.game, color), this.analysed.analysis) zip
      Accuracy.diffsList(Pov(this.analysed.game, !color), this.analysed.analysis)).exists(_ < -50)
  }

  def highBlurRate(color: Color): Boolean =
    this.analysed.game.playerBlurPercent(color) > 90

  def moderateBlurRate(color: Color): Boolean =
    this.analysed.game.playerBlurPercent(color) > 70

  def consistentMoveTimes(color: Color): Boolean =
    moveTimes(color).toNel.map(coefVariation).fold(false)(_ < 0.8)

  def noFastMoves(color: Color): Boolean = !moveTimes(color).exists(_ < 10)

  def suspiciousHoldAlert(color: Color): Boolean =
    this.analysed.game.player(color).hasSuspiciousHoldAlert

  def rankCheating(color: Color): Int =
    ((
      suspiciousErrorRate(color),
      alwaysHasAdvantage(color),
      highBlurRate(color),
      moderateBlurRate(color),
      consistentMoveTimes(color),
      noFastMoves(color),
      suspiciousHoldAlert(color)
    ) match {
        //  SF1    SF2    BLR1   BLR2   MTs1   MTs2   Holds
      case (true,  true,  true,  true,  true,  true,  true)   => 5 // all true, obvious cheat
      case (true,  _,     _,     _,     _,     true,  true)   => 5 // high accuracy, no fast moves, hold alerts
      case (true,  _,     true,  _,     _,     true,  _)      => 5 // high accuracy, high blurs, no fast moves
      case (true,  _,     _,     _,     true,  true,  _)      => 5 // high accuracy, consistent move times, no fast moves
      case (_,     true,  _,     _,     _,     _,     true)   => 4 // always has advantage, hold alerts
      case (_,     true,  true,  _,     _,     _,     _)      => 4 // always has advantage, high blurs
      case (true,  _,     _,     false, false, true,  _)      => 3 // high accuracy, no fast moves, but doesn't blur or flat line
      case (true,  _,     _,     _,     _,     false, _)      => 2 // high accuracy, but has fast moves
      case (false, false, _,     _,     _,    _,      _)      => 1 // low accuracy, doesn't hold advantage
      case (false, false, false, false, false, false, false)  => 1 // all false, obviously not cheating
      case _ => 1
    }).min(this.analysed.game.wonBy(color) match {
      case Some(c) if (c) => 5
      case _ => 3
    })

  def sfAvg(color: Color): Int = listAverage(Accuracy.diffsList(Pov(this.analysed.game, color), this.analysed.analysis)).toInt
  def sfSd(color: Color): Int = listDeviation(Accuracy.diffsList(Pov(this.analysed.game, color), this.analysed.analysis)).toInt
  def mtAvg(color: Color): Int = listAverage(skip(this.analysed.game.moveTimes.toList, {if (color == Color.White) 0 else 1})).toInt
  def mtSd(color: Color): Int = listDeviation(skip(this.analysed.game.moveTimes.toList, {if (color == Color.White) 0 else 1})).toInt
  def blurs(color: Color): Int = this.analysed.game.playerBlurPercent(color)
  def hold(color: Color): Boolean = this.analysed.game.player(color).hasSuspiciousHoldAlert

  def playerAssessment(color: Color): PlayerAssessment = PlayerAssessment(
    _id = this.analysed.game.id + "/" + color.name,
    gameId = this.analysed.game.id,
    userId = this.analysed.game.player(color).userId.getOrElse(""),
    white = (color == Color.White),
    assessment = rankCheating(color),
    date = DateTime.now,
    // meta
    sfAvg = sfAvg(color),
    sfSd = sfSd(color),
    mtAvg = mtAvg(color),
    mtSd = mtSd(color),
    blurs = blurs(color),
    hold = hold(color)
    )

  val assessments: GameAssessments = GameAssessments(
    white = Some(playerAssessment(Color.White)),
    black = Some(playerAssessment(Color.Black)))
}

object Display {
  def assessmentString(x: Int): String =
    x match {
      case 1 => "Not cheating"
      case 2 => "Unlikely cheating"
      case 3 => "Unclear"
      case 4 => "Likely cheating"
      case 5 => "Cheating"
      case 6 => "Undef"
    }
}

object Statistics {
  import Erf._
  import scala.annotation._

  def variance[T](a: NonEmptyList[T], optionalAvg: Option[T] = None)(implicit n: Numeric[T]): Double = {
    val avg: Double = optionalAvg.fold(average(a)){n.toDouble}

    a.map( i => pow(n.toDouble(i) - avg, 2)).list.sum / a.length
  }

  def deviation[T](a: NonEmptyList[T], optionalAvg: Option[T] = None)(implicit n: Numeric[T]): Double = sqrt(variance(a, optionalAvg))

  def average[T](a: NonEmptyList[T])(implicit n: Numeric[T]): Double = {
    @tailrec def average(a: List[T], sum: T = n.zero, depth: Int = 0): Double = {
      a match {
        case List()  => n.toDouble(sum) / depth
        case x :: xs => average(xs, n.plus(sum, x), depth + 1)
      }
    }
    average(a.list)
  }

  // Coefficient of Variance
  def coefVariation(a: NonEmptyList[Int]): Double = sqrt(variance(a)) / average(a)

  def intervalToVariance4(interval: Double): Double = pow(interval / 3, 8) // roughly speaking

  // Accumulative probability function for normal distributions
  def cdf[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double =
    0.5 * (1 + erf(n.toDouble(n.minus(x, avg)) / (n.toDouble(sd)*sqrt(2))))

  // The probability that you are outside of abs(x-n) from the mean on both sides
  def confInterval[T](x: T, avg: T, sd: T)(implicit n: Numeric[T]): Double =
    1 - cdf(n.abs(x), avg, sd) + cdf(n.times(n.fromInt(-1), n.abs(x)), avg, sd)

  def skip[A](l: List[A], n: Int) =
    l.zipWithIndex.collect {case (e,i) if ((i+n) % 2) == 0 => e} // (i+1) because zipWithIndex is 0-based

  def listSum(xs: List[Int]): Int = xs match {
    case Nil => 0
    case x :: tail => x + listSum(tail)
  }

  def listSum(xs: List[Double]): Double = xs match {
    case Nil => 0
    case x :: tail => x + listSum(tail)
  }

  def listAverage[T](x: List[T])(implicit n: Numeric[T]): Double = x match {
    case Nil      => 0
    case a :: Nil => n.toDouble(a)
    case a :: b   => average(NonEmptyList.nel(a, b))
  }

  def listDeviation[T](x: List[T])(implicit n: Numeric[T]): Double = x match {
    case Nil      => 0
    case _ :: Nil => 0
    case a :: b   => deviation(NonEmptyList.nel(a, b))
  }
}

object Erf {
  // constants
  val a1: Double =  0.254829592
  val a2: Double = -0.284496736
  val a3: Double =  1.421413741
  val a4: Double = -1.453152027
  val a5: Double =  1.061405429
  val p: Double  =  0.3275911

  def erf(x: Double): Double = {
    // Save the sign of x
    val sign = if (x < 0) -1 else 1
    val absx = abs(x)

    // A&S formula 7.1.26, rational approximation of error function
    val t = 1.0/(1.0 + p*absx);
    val y = 1.0 - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*exp(-x*x);
    sign*y
  }
}