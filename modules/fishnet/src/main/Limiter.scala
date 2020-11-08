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
      case Work.Sender(Some(userId), _, _, _) =>
        !analysisColl.exists(
          $doc(
            "sender.userId" -> userId
          )
        )
      case Work.Sender(_, Some(ip), _, _) =>
        !analysisColl.exists(
          $doc(
            "sender.ip" -> ip
          )
        )
      case _ => fuFalse
    }

  private val maxPerDay  = 25
  private val maxPerWeek = 80

  private def perDayCheck(sender: Work.Sender) =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(Some(userId), _, _, _) =>
        requesterApi.countTodayAndThisWeek(userId) map { case (daily, weekly) =>
          weekly < maxPerWeek &&
            daily < (if (weekly < maxPerWeek / 2) maxPerDay else maxPerDay / 2)
        }
      case Work.Sender(_, Some(ip), _, _) =>
        fuccess {
          RequestLimitPerIP(ip, cost = 1)(true)(false)
        }
      case _ => fuFalse
    }
}
