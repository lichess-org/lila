package lila.evaluation

import chess.Color
import org.joda.time.DateTime

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

case class PlayerAggregateAssessment(playerAssessments: List[PlayerAssessment],
  relatedUsers: List[String],
  relatedCheaters: List[String]) {
  import Statistics._
  import AccountAction._

  def action = {
    val markable: Boolean = (
      (cheatingSum >= 2 || cheatingSum + likelyCheatingSum >= 4)
      // more than 5 percent of games are cheating
      && (cheatingSum.toDouble / assessmentsCount >= 0.05 - relationModifier
      // or more than 10 percent of games are likely cheating
        || (cheatingSum + likelyCheatingSum).toDouble / assessmentsCount >= 0.10 - relationModifier)
    )

    val reportable: Boolean = (
      (cheatingSum >= 1 || cheatingSum + likelyCheatingSum >= 2)
      // more than 2 percent of games are cheating
      && (cheatingSum.toDouble / assessmentsCount >= 0.02 - relationModifier
      // or more than 5 percent of games are likely cheating
        || (cheatingSum + likelyCheatingSum).toDouble / assessmentsCount >= 0.05 - relationModifier)
    )

    val bannable: Boolean = (relatedCheatersCount == relatedUsersCount) && relatedUsersCount >= 1

    val suspicious = List(sfAvgBlurs,   sfAvgLowVar,  sfAvgHold)
    val normal     = List(sfAvgNoBlurs, sfAvgHighVar, sfAvgNoHold)

    val actionable: Boolean = {
      def sigDif(a: Option[Int], b: Option[Int]): Option[Boolean] = (a, b) match {
        case (Some(a), Some(b)) => Some(b - a > 10)
        case _ => none
      }

      val difs = (suspicious zip normal).map(a => sigDif(a._1, a._2))

      difs.forall(_.isEmpty) || difs.exists(~_) || assessmentsCount < 50
    }

    val exceptional: Boolean =
      suspicious.exists{
        case Some(a) => a < 15
        case none    => false
      }

    if (actionable) {
      if (markable && bannable) EngineAndBan
      else if (markable)        Engine
      else if (reportable
        && exceptional
        && cheatingSum >= 1)    Engine
      else                      Report
    } else {
      if (markable)             Report
      else if (reportable)      Report
      else                      Nothing
    }
  }

  def countAssessmentValue(assessment: Int) = listSum(playerAssessments map {
    case a if (a.assessment == assessment) => 1
    case _ => 0
  })

  val relatedCheatersCount = relatedCheaters.distinct.size
  val relatedUsersCount = relatedUsers.distinct.size
  val assessmentsCount = playerAssessments.size match {
    case 0 => 1
    case a => a
  }
  val relationModifier = if (relatedUsersCount >= 1) 0.02 else 0 
  val cheatingSum = countAssessmentValue(5)
  val likelyCheatingSum = countAssessmentValue(4)


  // Some statistics
  def sfAvgGiven(predicate: PlayerAssessment => Boolean): Option[Int] = {
    val avg = listAverage(playerAssessments.filter(predicate).map(_.sfAvg)).toInt
    if (playerAssessments.exists(predicate)) Some(avg) else none
  }

  // Average SF Avg given blur rate
  val sfAvgBlurs     = sfAvgGiven(_.blurs > 70)
  val sfAvgNoBlurs   = sfAvgGiven(_.blurs <= 70)

  // Average SF Avg given move time coef of variance
  val sfAvgLowVar    = sfAvgGiven(a => a.mtSd.toDouble / a.mtAvg < 0.5)
  val sfAvgHighVar   = sfAvgGiven(a => a.mtSd.toDouble / a.mtAvg >= 0.5)

  // Average SF Avg given bot
  val sfAvgHold      = sfAvgGiven(_.hold)
  val sfAvgNoHold    = sfAvgGiven(!_.hold)

  def reportText(maxGames: Int = 10): String = {
    val gameLinks: String = (playerAssessments.sortBy(-_.assessment).take(maxGames).map{ a =>
      Display.emoticon(a.assessment) + " http://lichess.org/" + a.gameId + "/" + a.color.name
    }).mkString("\n")

    s"""[AUTOREPORT]
    Cheating Games: $cheatingSum
    Likely Cheating Games: $likelyCheatingSum

    $gameLinks"""
  }
}

case class PlayerFlags(
  suspiciousErrorRate: Boolean,
  alwaysHasAdvantage: Boolean,
  highBlurRate: Boolean,
  moderateBlurRate: Boolean,
  consistentMoveTimes: Boolean,
  noFastMoves: Boolean,
  suspiciousHoldAlert: Boolean)
