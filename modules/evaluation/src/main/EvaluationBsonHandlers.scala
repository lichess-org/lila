package lila.evaluation

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

object EvaluationBsonHandlers:

  given BSON[PlayerFlags] with
    def reads(r: BSON.Reader): PlayerFlags =
      PlayerFlags(
        suspiciousErrorRate = r.boolD("ser"),
        alwaysHasAdvantage = r.boolD("aha"),
        highBlurRate = r.boolD("hbr"),
        moderateBlurRate = r.boolD("mbr"),
        highlyConsistentMoveTimes = r.boolD("hcmt"),
        moderatelyConsistentMoveTimes = r.boolD("cmt"),
        noFastMoves = r.boolD("nfm"),
        suspiciousHoldAlert = r.boolD("sha")
      )
    def writes(w: BSON.Writer, o: PlayerFlags) =
      $doc(
        "ser" -> w.boolO(o.suspiciousErrorRate),
        "aha" -> w.boolO(o.alwaysHasAdvantage),
        "hbr" -> w.boolO(o.highBlurRate),
        "mbr" -> w.boolO(o.moderateBlurRate),
        "hcmt" -> w.boolO(o.highlyConsistentMoveTimes),
        "cmt" -> w.boolO(o.moderatelyConsistentMoveTimes),
        "nfm" -> w.boolO(o.noFastMoves),
        "sha" -> w.boolO(o.suspiciousHoldAlert)
      )

  given BSONHandler[GameAssessment] = BSONIntegerHandler.as[GameAssessment](GameAssessment.orDefault, _.id)

  given BSON[PlayerAssessment] with
    def reads(r: BSON.Reader): PlayerAssessment = PlayerAssessment(
      _id = r.str("_id"),
      gameId = r.get[GameId]("gameId"),
      userId = r.get[UserId]("userId"),
      color = Color.fromWhite(r.bool("white")),
      assessment = r.get[GameAssessment]("assessment"),
      date = r.date("date"),
      basics = PlayerAssessment.Basics(
        moveTimes = Statistics.IntAvgSd(
          avg = r.int("mtAvg"),
          sd = r.int("mtSd")
        ),
        hold = r.bool("hold"),
        blurs = r.int("blurs"),
        blurStreak = r.intO("blurStreak"),
        mtStreak = r.boolD("mtStreak")
      ),
      analysis = Statistics.IntAvgSd(
        avg = r.int("sfAvg"),
        sd = r.int("sfSd")
      ),
      flags = r.get[PlayerFlags]("flags"),
      tcFactor = r.doubleO("tcFactor")
    )
    def writes(w: BSON.Writer, o: PlayerAssessment) =
      $doc(
        "_id" -> o._id,
        "gameId" -> o.gameId,
        "userId" -> o.userId,
        "white" -> o.color.white,
        "assessment" -> o.assessment,
        "date" -> o.date,
        "flags" -> o.flags,
        "sfAvg" -> o.analysis.avg,
        "sfSd" -> o.analysis.sd,
        "mtAvg" -> o.basics.moveTimes.avg,
        "mtSd" -> o.basics.moveTimes.sd,
        "blurs" -> o.basics.blurs,
        "hold" -> o.basics.hold,
        "blurStreak" -> o.basics.blurStreak,
        "mtStreak" -> w.boolO(o.basics.mtStreak),
        "tcFactor" -> o.tcFactor
      )
