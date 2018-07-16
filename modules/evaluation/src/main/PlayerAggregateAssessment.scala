package lila.evaluation

import chess.Color
import lila.user.User
import org.joda.time.DateTime

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
    hold: Boolean
) {

  val color = Color(white)
}

case class PlayerAggregateAssessment(
    user: User,
    playerAssessments: List[PlayerAssessment],
    relatedUsers: List[String],
    relatedCheaters: Set[String]
) {
  import Statistics._
  import AccountAction._
  import GameAssessment.{ Cheating, LikelyCheating }

  def action: AccountAction = {

    def percentCheatingGames(x: Double) =
      cheatingSum.toDouble / assessmentsCount >= (x / 100) - relationModifier

    def percentLikelyCheatingGames(x: Double) =
      (cheatingSum + likelyCheatingSum).toDouble / assessmentsCount >= (x / 100) - relationModifier
      
    def percentUnclearGames(x: Double) =
      (cheatingSum + likelyCheatingSum + unclearSum).toDouble / assessmentsCount >= (x / 100) - relationModifier

    val markable: Boolean = !isGreatUser && isWorthLookingAt &&
      (cheatingSum >= 3 || cheatingSum + likelyCheatingSum >= 6) &&
      (percentCheatingGames(10) || percentLikelyCheatingGames(20))

    val reportable: Boolean = isWorthLookingAt &&
      (cheatingSum >= 2
       || cheatingSum + likelyCheatingSum >= isNewRatedUser.fold(2, 4)
       || cheatingSum + likelyCheatingSum + unclearSum >= isNewRatedUser.fold(4, 8)) &&
      (percentCheatingGames(5) || percentLikelyCheatingGames(10) || percentUnclearGames(15))

    val bannable: Boolean = (relatedCheatersCount == relatedUsersCount) && relatedUsersCount >= 1

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

  val relatedCheatersCount = relatedCheaters.size
  val relatedUsersCount = relatedUsers.distinct.size
  val assessmentsCount = playerAssessments.size match {
    case 0 => 1
    case a => a
  }
  val relationModifier = if (relatedUsersCount >= 1) 0.02 else 0
  val cheatingSum = countAssessmentValue(Cheating)
  val likelyCheatingSum = countAssessmentValue(LikelyCheating)
  val unclearSum = countAssessmentValue(Unclear)

  // Some statistics
  def sfAvgGiven(predicate: PlayerAssessment => Boolean): Option[Int] = {
    val avg = listAverage(playerAssessments.filter(predicate).map(_.sfAvg)).toInt
    if (playerAssessments.exists(predicate)) Some(avg) else none
  }

  // Average SF Avg given blur rate
  val sfAvgBlurs = sfAvgGiven(_.blurs > 70)
  val sfAvgNoBlurs = sfAvgGiven(_.blurs <= 70)

  // Average SF Avg given move time coef of variance
  val sfAvgLowVar = sfAvgGiven(a => a.mtSd.toDouble / a.mtAvg < 0.5)
  val sfAvgHighVar = sfAvgGiven(a => a.mtSd.toDouble / a.mtAvg >= 0.5)

  // Average SF Avg given bot
  val sfAvgHold = sfAvgGiven(_.hold)
  val sfAvgNoHold = sfAvgGiven(!_.hold)

  def isGreatUser = user.perfs.bestRating > 2200 && user.count.rated >= 100

  def isNewRatedUser = user.count.rated < 10

  def isWorthLookingAt = user.count.rated >= 2

  def reportText(reason: String, maxGames: Int = 10): String = {
    val gameLinks: String = (playerAssessments.sortBy(-_.assessment.id).take(maxGames).map { a =>
      a.assessment.emoticon + " lichess.org/" + a.gameId + "/" + a.color.name
    }).mkString("\n")

    s"""[AUTOREPORT] $reason
    Cheating Games: $cheatingSum
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
    consistentMoveTimes: Boolean,
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
      consistentMoveTimes = r boolD "cmt",
      noFastMoves = r boolD "nfm",
      suspiciousHoldAlert = r boolD "sha"
    )

    def writes(w: BSON.Writer, o: PlayerFlags) = BSONDocument(
      "ser" -> w.boolO(o.suspiciousErrorRate),
      "aha" -> w.boolO(o.alwaysHasAdvantage),
      "hbr" -> w.boolO(o.highBlurRate),
      "mbr" -> w.boolO(o.moderateBlurRate),
      "cmt" -> w.boolO(o.consistentMoveTimes),
      "nfm" -> w.boolO(o.noFastMoves),
      "sha" -> w.boolO(o.suspiciousHoldAlert)
    )
  }
}
