package lila.evaluation

import chess.Color
import lila.user.User
import org.joda.time.DateTime
import scala.math.sqrt

case class PlayerAssessment(
    _id: String,
    gameId: String,
    userId: String,
    white: Boolean,
    assessment: GameAssessment,
    date: DateTime,
    // meta
    flags: PlayerFlags,
    sfAvg: Int,
    sfSd: Int,
    mtAvg: Int,
    mtSd: Int,
    blurs: Int,
    hold: Boolean,
    blurStreak: Option[Int],
    mtStreak: Option[Boolean]
) {

  val color = Color(white)
}

case class PlayerAggregateAssessment(
    user: User,
    playerAssessments: List[PlayerAssessment]
) {
  import Statistics._
  import AccountAction._
  import GameAssessment.{ Cheating, LikelyCheating }

  def action: AccountAction = {

    def percentCheatingGames(x: Double) =
      cheatingSum.toDouble / assessmentsCount >= (x / 100)

    def percentLikelyCheatingGames(x: Double) =
      (cheatingSum + likelyCheatingSum).toDouble / assessmentsCount >= (x / 100)

    val markable: Boolean = !isGreatUser && isWorthLookingAt &&
      (cheatingSum >= 3 || cheatingSum + likelyCheatingSum >= 6) &&
      (percentCheatingGames(10) || percentLikelyCheatingGames(20))

    val reportable: Boolean = isWorthLookingAt &&
      (cheatingSum >= 2 || cheatingSum + likelyCheatingSum >= (if (isNewRatedUser) 2 else 4)) &&
      (percentCheatingGames(5) || percentLikelyCheatingGames(10))

    val bannable: Boolean = false

    def sigDif(dif: Int)(a: Option[Int], b: Option[Int]): Option[Boolean] =
      (a |@| b) apply { case (a, b) => b - a > dif }

    val difs = List(
      (sfAvgBlurs, sfAvgNoBlurs),
      (sfAvgLowVar, sfAvgHighVar),
      (sfAvgHold, sfAvgNoHold)
    )

    val actionable: Boolean = {
      val difFlags = difs map (sigDif(10)_).tupled
      difFlags.forall(_.isEmpty) || difFlags.exists(~_) || assessmentsCount < 50
    }

    def exceptionalDif: Boolean = difs map (sigDif(30)_).tupled exists (~_)

    if (actionable) {
      if (markable && bannable) EngineAndBan
      else if (markable) Engine
      else if (reportable && exceptionalDif && cheatingSum >= 1) Engine
      else if (reportable) reportVariousReasons
      else Nothing
    } else {
      if (markable) reportVariousReasons
      else if (reportable) reportVariousReasons
      else Nothing
    }
  }

  def countAssessmentValue(assessment: GameAssessment) = playerAssessments count {
    _.assessment == assessment
  }

  val assessmentsCount = playerAssessments.size match {
    case 0 => 1
    case a => a
  }
  val cheatingSum = countAssessmentValue(Cheating)
  val likelyCheatingSum = countAssessmentValue(LikelyCheating)

  // Some statistics
  def sfAvgGiven(predicate: PlayerAssessment => Boolean): Option[Int] = {
    val avg = listAverage(playerAssessments.filter(predicate).map(_.sfAvg)).toInt
    if (playerAssessments.exists(predicate)) Some(avg) else none
  }
  
  def sfCiGiven(predicate: PlayerAssessment => Boolean): Option[Array[Int]] = {
    val avg = listAverage(playerAssessments.filter(predicate).map(_.sfAvg))
    val n = playerAssessments.filter(predicate).length
    // listDeviation uses stdDev, which does not use Bessel correction and happily returns
    // a standard deviation of 0 if n = 1. This is not great for calculating a CI.
    // Sample standard deviation should be undefined for n = 1, but our purpose it makes sense to simply assume a high value of 100
    val width = if (n <= 1) 196 else listDeviation(playerAssessments.filter(predicate).map(_.sfAvg)) / sqrt(n) * 1.96
    if (playerAssessments.exists(predicate)) Some(Array((avg - width).toInt, (avg + width).toInt)) else none
  }

  // Average SF Avg and CI given blur rate
  val sfAvgBlurs = sfAvgGiven(_.blurs > 70)
  val sfAvgNoBlurs = sfAvgGiven(_.blurs <= 70)

  val sfCiBlurs = sfCiGiven(_.blurs > 70)
  val sfCiNoBlurs = sfCiGiven(_.blurs <= 70)

  // Average SF Avg and CI given move time coef of variance
  val sfAvgLowVar = sfAvgGiven(a => a.mtSd.toDouble / a.mtAvg < 0.5)
  val sfAvgHighVar = sfAvgGiven(a => a.mtSd.toDouble / a.mtAvg >= 0.5)

  val sfCiLowVar = sfCiGiven(a => a.mtSd.toDouble / a.mtAvg < 0.5)
  val sfCiHighVar = sfCiGiven(a => a.mtSd.toDouble / a.mtAvg >= 0.5)

  // Average SF Avg and CI given bot
  val sfAvgHold = sfAvgGiven(_.hold)
  val sfAvgNoHold = sfAvgGiven(!_.hold)

  val sfCiHold = sfCiGiven(_.hold)
  val sfCiNoHold = sfCiGiven(!_.hold)

  def isGreatUser = user.perfs.bestRating > 2200 && user.count.rated >= 100

  def isNewRatedUser = user.count.rated < 10

  def isWorthLookingAt = user.count.rated >= 2

  def reportText(reason: String, maxGames: Int = 10): String = {
    val gameLinks: String = (playerAssessments.sortBy(-_.assessment.id).take(maxGames).map { a =>
      a.assessment.emoticon + " lichess.org/" + a.gameId + "/" + a.color.name
    }).mkString("\n")

    s"""Cheating Games: $cheatingSum
    Likely Cheating Games: $likelyCheatingSum
    $gameLinks"""
  }
}

object PlayerAggregateAssessment {

  case class WithGames(pag: PlayerAggregateAssessment, games: List[lila.game.Game]) {
    def pov(pa: PlayerAssessment) = games find (_.id == pa.gameId) map { lila.game.Pov(_, pa.color) }
  }
}

case class PlayerFlags(
    suspiciousErrorRate: Boolean,
    alwaysHasAdvantage: Boolean,
    highBlurRate: Boolean,
    moderateBlurRate: Boolean,
    highlyConsistentMoveTimes: Boolean,
    moderatelyConsistentMoveTimes: Boolean,
    noFastMoves: Boolean,
    suspiciousHoldAlert: Boolean
)

object PlayerFlags {

  import reactivemongo.bson._
  import lila.db.BSON

  implicit val playerFlagsBSONHandler = new BSON[PlayerFlags] {

    def reads(r: BSON.Reader): PlayerFlags = PlayerFlags(
      suspiciousErrorRate = r boolD "ser",
      alwaysHasAdvantage = r boolD "aha",
      highBlurRate = r boolD "hbr",
      moderateBlurRate = r boolD "mbr",
      highlyConsistentMoveTimes = r boolD "hcmt",
      moderatelyConsistentMoveTimes = r boolD "cmt",
      noFastMoves = r boolD "nfm",
      suspiciousHoldAlert = r boolD "sha"
    )

    def writes(w: BSON.Writer, o: PlayerFlags) = BSONDocument(
      "ser" -> w.boolO(o.suspiciousErrorRate),
      "aha" -> w.boolO(o.alwaysHasAdvantage),
      "hbr" -> w.boolO(o.highBlurRate),
      "mbr" -> w.boolO(o.moderateBlurRate),
      "hcmt" -> w.boolO(o.highlyConsistentMoveTimes),
      "cmt" -> w.boolO(o.moderatelyConsistentMoveTimes),
      "nfm" -> w.boolO(o.noFastMoves),
      "sha" -> w.boolO(o.suspiciousHoldAlert)
    )
  }
}
