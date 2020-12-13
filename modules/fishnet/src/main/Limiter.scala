package lila.fishnet

import reactivemongo.api.bson._
import scala.concurrent.duration._

import lila.common.IpAddress
import lila.db.dsl._

final private class Limiter(
    analysisColl: Coll,
    requesterApi: lila.analyse.RequesterApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(sender: Work.Sender, ignoreConcurrentCheck: Boolean): Fu[Boolean] =
    (fuccess(ignoreConcurrentCheck) >>| concurrentCheck(sender)) flatMap {
      case false => fuFalse
      case true  => perDayCheck(sender)
    }

  private val RequestLimitPerIP = new lila.memo.RateLimit[IpAddress](
    credits = 50,
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

  private val maxPerDay  = 30
  private val maxPerWeek = 120

  private def perDayCheck(sender: Work.Sender) =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(userId, ip, _, _) =>
        def perUser =
          requesterApi.countTodayAndThisWeek(userId) map { case (daily, weekly) =>
            weekly < maxPerWeek &&
              daily < (if (weekly < maxPerWeek / 2) maxPerDay else maxPerDay / 2)
          }
        ip.fold(perUser) { ipAddress =>
          RequestLimitPerIP(ipAddress, cost = 1)(perUser)(fuccess(false))
        }
      case _ => fuFalse
    }
}
