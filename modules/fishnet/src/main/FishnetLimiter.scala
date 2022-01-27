package lila.fishnet

import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.common.IpAddress
import lila.db.dsl._

final private class FishnetLimiter(
    analysisColl: Coll,
    requesterApi: lila.analyse.RequesterApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  import FishnetLimiter._

  def apply(sender: Work.Sender, ignoreConcurrentCheck: Boolean, ownGame: Boolean): Fu[Analyser.Result] =
    (fuccess(ignoreConcurrentCheck) >>| concurrentCheck(sender)) flatMap {
      case false => fuccess(Analyser.Result.ConcurrentAnalysis)
      case true  => perDayCheck(sender)
    } flatMap { result =>
      (result.ok ?? requesterApi.add(sender.userId, ownGame)) inject result
    }

  private val RequestLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 60,
    duration = 20 hours,
    key = "request_analysis.ip"
  )

  private def concurrentCheck(sender: Work.Sender) =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(userId, ip, _, _) =>
        !analysisColl.exists(
          $or(
            $doc("sender.ip"     -> ip),
            $doc("sender.userId" -> userId)
          )
        )
      case _ => fuFalse
    }

  private def perDayCheck(sender: Work.Sender): Fu[Analyser.Result] =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuccess(Analyser.Result.Ok)
      case Work.Sender(userId, ip, _, _) =>
        def perUser =
          requesterApi.countTodayAndThisWeek(userId) map { case (daily, weekly) =>
            if (weekly >= maxPerWeek) Analyser.Result.WeeklyLimit
            else if (daily >= (if (weekly < maxPerWeek * 2 / 3) maxPerDay else maxPerDay * 2 / 3))
              Analyser.Result.DailyLimit
            else Analyser.Result.Ok
          }
        ip.fold(perUser) { ipAddress =>
          RequestLimitPerIP(ipAddress, cost = 1)(perUser)(fuccess(Analyser.Result.DailyIpLimit))
        }
    }
}

object FishnetLimiter {
  val maxPerDay  = 40
  val maxPerWeek = 200
}
