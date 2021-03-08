package lila.evaluation

import cats.implicits._
import chess.Color
import org.joda.time.DateTime
import scala.math.sqrt

import lila.user.User
import lila.game.Game

case class PlayerAggregateAssessment(
    user: User,
    playerAssessments: List[PlayerAssessment]
) {
  import Statistics._
  import AccountAction._
  import GameAssessment.{ Cheating, LikelyCheating }

  def action: AccountAction = {

    def scoreCheatingGames(x: Double) =
      weightedCheatingSum / assessmentsCount >= (x / 100)

    def scoreLikelyCheatingGames(x: Double) =
      (weightedCheatingSum + weightedLikelyCheatingSum) / assessmentsCount >= (x / 100)

    val markable: Boolean = !user.hasTitle && !isGreatUser && isWorthLookingAt &&
      (weightedCheatingSum >= 3 || weightedCheatingSum + weightedLikelyCheatingSum >= 6) &&
      (scoreCheatingGames(8) || scoreLikelyCheatingGames(16))

    val reportable: Boolean = isWorthLookingAt &&
      (cheatingSum >= 2 || cheatingSum + likelyCheatingSum >= (if (isNewRatedUser) 2
                                                               else 4)) &&
      (scoreCheatingGames(5) || scoreLikelyCheatingGames(10))

    val bannable: Boolean = false

    def sigDif(dif: Int)(a: Option[(Int, Int, Int)], b: Option[(Int, Int, Int)]): Option[Boolean] =
      (a, b) mapN { (a, b) => b._1 - a._1 > dif }

    val difs = List(
      (sfAvgBlurs, sfAvgNoBlurs),
      (sfAvgLowVar, sfAvgHighVar),
      (sfAvgHold, sfAvgNoHold)
    )

    val actionable: Boolean = {
      val difFlags = difs map (sigDif(10) _).tupled
      difFlags.forall(_.isEmpty) || difFlags.exists(~_) || assessmentsCount < 50
    }

    if (actionable) {
      if (markable && bannable) EngineAndBan
      else if (markable) Engine
      else if (reportable) reportVariousReasons
      else Nothing
    } else {
      if (markable) reportVariousReasons
      else if (reportable) reportVariousReasons
      else Nothing
    }
  }

  def countAssessmentValue(assessment: GameAssessment) =
    playerAssessments count {
      _.assessment == assessment
    }

  val assessmentsCount = playerAssessments.size match {
    case 0 => 1
    case a => a
  }
  val cheatingSum       = countAssessmentValue(Cheating)
  val likelyCheatingSum = countAssessmentValue(LikelyCheating)

  def weightedAssessmentValue(assessment: GameAssessment): Double =
    playerAssessments map { pa =>
      if (pa.assessment != assessment) 0.0
      else pa.tcFactor.getOrElse(1.0) * (if (pa.flags.highlyConsistentMoveTimes) 1.6 else 1.0)
    } sum

  val weightedCheatingSum       = weightedAssessmentValue(Cheating)
  val weightedLikelyCheatingSum = weightedAssessmentValue(LikelyCheating)

  // Some statistics
  def sfAvgGiven(predicate: PlayerAssessment => Boolean): Option[(Int, Int, Int)] = {
    val filteredAssessments = playerAssessments.filter(predicate)
    val n                   = filteredAssessments.size
    if (n < 2) none
    else {
      val filteredSfAvg = filteredAssessments.map(_.analysis.avg)
      val avg           = listAverage(filteredSfAvg)
      // listDeviation does not apply Bessel's correction, so we do it here by using sqrt(n - 1) instead of sqrt(n)
      val width = listDeviation(filteredSfAvg) / sqrt(n - 1) * 1.96
      Some((avg.toInt, (avg - width).toInt, (avg + width).toInt))
    }
  }

  // Average SF Avg and CI given blur rate
  val sfAvgBlurs   = sfAvgGiven(_.basics.blurs > 70)
  val sfAvgNoBlurs = sfAvgGiven(_.basics.blurs <= 70)

  // Average SF Avg and CI given move time coef of variance
  val sfAvgLowVar  = sfAvgGiven(a => a.basics.moveTimes.sd.toDouble / a.basics.moveTimes.avg < 0.5)
  val sfAvgHighVar = sfAvgGiven(a => a.basics.moveTimes.sd.toDouble / a.basics.moveTimes.avg >= 0.5)

  // Average SF Avg and CI given bot
  val sfAvgHold   = sfAvgGiven(_.basics.hold)
  val sfAvgNoHold = sfAvgGiven(!_.basics.hold)

  def isGreatUser = user.perfs.bestRating > 2500 && user.count.rated >= 100

  def isNewRatedUser = user.count.rated < 10

  def isWorthLookingAt = user.count.rated >= 2

  def reportText(maxGames: Int = 10): String = {
    val gameLinks: String = playerAssessments
      .sortBy(-_.assessment.id)
      .take(maxGames)
      .map { a =>
        a.assessment.emoticon + " lichess.org/" + a.gameId + "/" + a.color.name
      }
      .mkString("\n")

    s"""Cheating Games: $cheatingSum (weighted: $weightedCheatingSum)
    Likely Cheating Games: $likelyCheatingSum (weighted: $weightedLikelyCheatingSum)
    $gameLinks"""
  }
}

object PlayerAggregateAssessment {

  case class WithGames(pag: PlayerAggregateAssessment, games: List[lila.game.Game]) {
    def pov(pa: PlayerAssessment) = games find (_.id == pa.gameId) map { lila.game.Pov(_, pa.color) }
  }
}
