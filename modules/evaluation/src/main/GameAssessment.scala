package lila.evaluation

import chess.{ Color }
import lila.game.{ Pov, Game }
import lila.analyse.{ Accuracy, Analysis }
import lila.user.User
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
  flags: PlayerFlags,
  sfAvg: Int,
  sfSd: Int,
  mtAvg: Int,
  mtSd: Int,
  blurs: Int,
  hold: Boolean
  ) {
  val color = Color.apply(white)
}

sealed trait AccountAction {
  val description: String
  val colorClass: String
  override def toString = description
}

object AccountAction {
  case object EngineAndBan extends AccountAction {
    val description: String = "Mark and IP ban"
    val colorClass = "4"
  }
  case object Engine extends AccountAction {
    val description: String = "Mark as engine"
    val colorClass = "3"
  }
  case object Report extends AccountAction {
    val description: String = "Report to mods"
    val colorClass = "2"
  }
  case object Nothing extends AccountAction {
    val description: String = "Not suspicious"
    val colorClass = "1"
  }
}

case class PlayerAggregateAssessment(playerAssessments: List[PlayerAssessment],
  user: User,
  relatedUsers: List[String],
  relatedCheaters: List[String]) {
  import Statistics._

  def action = (cheatingSum, likelyCheatingSum, daysOld, relatedCheatersCount, relatedUsersCount) match {
    // New account, some cheating games, strictly related to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 2 || cs + lcs >= 4)  && pt < 1 && rc >= 1 && rc == ru)  => AccountAction.EngineAndBan
    // Older account, many cheating games, has strict relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 8 || cs + lcs >= 15) && pt >= 1 && rc >= 1 && rc == ru) => AccountAction.EngineAndBan

    // New account, some cheating games, has relation to cheating accounts but is non-strict
    case (cs, lcs, pt, rc, ru) if ((cs >= 2 || cs + lcs >= 4) && pt < 1 && rc >= 1)               => AccountAction.Engine
    // Older account, many cheating games, has relation to cheating accounts but is non-strict
    case (cs, lcs, pt, rc, ru) if ((cs >= 8 || cs + lcs >= 15) && pt >= 1 && rc >= 1)             => AccountAction.Engine
    // Much older account, lots of cheating games, has relation to cheating accounts but is non-strict
    case (cs, lcs, pt, rc, ru) if ((cs >= 15 || cs + lcs >= 30) && pt >= 5 && rc >= 1)            => AccountAction.Engine

    // New account, some cheating games, no relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 2 || cs + lcs >= 4) && pt < 1 && rc == 0)               => AccountAction.Engine
    // Older account, many cheating games, no relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 15 || cs + lcs >= 30) && pt >= 1 && rc == 0)            => AccountAction.Engine
    // Much older account, lots of cheating games, no relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 30 || cs + lcs >= 60) && pt >= 5 && rc == 0)            => AccountAction.Engine

    // New account, some cheating games, has relation to cheating accounts but is non-strict
    case (cs, lcs, pt, rc, ru) if ((cs >= 1 || cs + lcs >= 2) && pt < 1 && rc >= 1)               => AccountAction.Report
    // Older account, many cheating games, has relation to cheating accounts but is non-strict
    case (cs, lcs, pt, rc, ru) if ((cs >= 2 || cs + lcs >= 4) && pt >= 1 && rc >= 1)              => AccountAction.Report
    // Much older account, lots of cheating games, has relation to cheating accounts but is non-strict
    case (cs, lcs, pt, rc, ru) if ((cs >= 4 || cs + lcs >= 8) && pt >= 5 && rc >= 1)              => AccountAction.Report

    // New account, some cheating games, no relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 1 || cs + lcs >= 2) && pt < 1 && rc == 0)               => AccountAction.Report
    // Older account, many cheating games, no relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 4 || cs + lcs >= 8) && pt >= 1 && rc == 0)              => AccountAction.Report
    // Much older account, lots of cheating games, no relation to cheating accounts
    case (cs, lcs, pt, rc, ru) if ((cs >= 8 || cs + lcs >= 15) && pt >= 5 && rc == 0)             => AccountAction.Report

    // Anything else
    case _ => AccountAction.Nothing
  }

  def countAssessmentValue(assessment: Int) = listSum(playerAssessments map {
    case a if (a.assessment == assessment) => 1
    case _ => 0
  })
  val daysOld = user.playTime.fold(0){ _.totalPeriod.toStandardDays.getDays }
  val relatedCheatersCount = relatedCheaters.distinct.size
  val relatedUsersCount = relatedUsers.distinct.size
  val cheatingSum = countAssessmentValue(5)
  val likelyCheatingSum = countAssessmentValue(4)
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

case class PlayerFlags(
  suspiciousErrorRate: Boolean,
  alwaysHasAdvantage: Boolean,
  highBlurRate: Boolean,
  moderateBlurRate: Boolean,
  consistentMoveTimes: Boolean,
  noFastMoves: Boolean,
  suspiciousHoldAlert: Boolean)

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
    moveTimes(color).toNel.map(coefVariation).fold(false)(_ < 0.5)

  def noFastMoves(color: Color): Boolean = !moveTimes(color).exists(_ < 10)

  def suspiciousHoldAlert(color: Color): Boolean =
    this.analysed.game.player(color).hasSuspiciousHoldAlert

  def flags(color: Color): PlayerFlags = PlayerFlags(
    suspiciousErrorRate(color),
    alwaysHasAdvantage(color),
    highBlurRate(color),
    moderateBlurRate(color),
    consistentMoveTimes(color),
    noFastMoves(color),
    suspiciousHoldAlert(color)
  )

  def rankCheating(color: Color): Int =
    (flags(color) match {
                   //  SF1    SF2    BLR1   BLR2   MTs1   MTs2   Holds
      case PlayerFlags(true,  true,  true,  true,  true,  true,  true)   => 5 // all true, obvious cheat
      case PlayerFlags(true,  _,     _,     _,     _,     true,  true)   => 5 // high accuracy, no fast moves, hold alerts
      case PlayerFlags(_,     true,  _,     _,     _,     true,  true)   => 5 // always has advantage, no fast moves, hold alerts
      case PlayerFlags(true,  _,     true,  _,     _,     true,  _)      => 5 // high accuracy, high blurs, no fast moves
      
      case PlayerFlags(true,  _,     _,     _,     true,  true,  _)      => 4 // high accuracy, consistent move times, no fast moves
      case PlayerFlags(true,  _,     _,     true,  _,     true,  _)      => 4 // high accuracy, moderate blurs, no fast moves
      case PlayerFlags(_,     true,  _,     true,  true,  _,     _)      => 4 // always has advantage, moderate blurs, highly consistent move times
      case PlayerFlags(_,     true,  _,     _,     _,     _,     true)   => 4 // always has advantage, hold alerts
      case PlayerFlags(_,     true,  true,  _,     _,     _,     _)      => 4 // always has advantage, high blurs

      case PlayerFlags(true,  _,     _,     false, false, true,  _)      => 3 // high accuracy, no fast moves, but doesn't blur or flat line

      case PlayerFlags(true,  _,     _,     _,     _,     false, _)      => 2 // high accuracy, but has fast moves

      case PlayerFlags(false, false, _,     _,     _,    _,      _)      => 1 // low accuracy, doesn't hold advantage
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

  def playerAssessment(color: Color): PlayerAssessment =
    PlayerAssessment(
    _id = this.analysed.game.id + "/" + color.name,
    gameId = this.analysed.game.id,
    userId = this.analysed.game.player(color).userId.getOrElse(""),
    white = (color == Color.White),
    assessment = rankCheating(color),
    date = DateTime.now,
    // meta
    flags = flags(color),
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
      case _ => "Undef"
    }

  def stockfishSig(pa: PlayerAssessment): Int =
    (pa.flags.suspiciousErrorRate, pa.flags.alwaysHasAdvantage) match {
      case (true, true)  => 5
      case (true, false) => 4
      case (false, true) => 3
      case _             => 1
    }

  def moveTimeSig(pa: PlayerAssessment): Int =
    (pa.flags.consistentMoveTimes, pa.flags.noFastMoves) match {
      case (true, true)  => 5
      case (true, false) => 4
      case (false, true) => 3
      case _             => 1
    }

  def blurSig(pa: PlayerAssessment): Int =
    (pa.flags.highBlurRate, pa.flags.moderateBlurRate) match {
      case (true, _) => 5
      case (_, true) => 4
      case _         => 1
    }

  def holdSig(pa: PlayerAssessment): Int = if (pa.flags.suspiciousHoldAlert) 5 else 1

  def emoticon(assessment: Int): String = assessment match {
    case 5 => ">:("
    case 4 => ":("
    case 3 => ":|"
    case 2 => ":)"
    case 1 => ":D"
    case _ => "\\o/"
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