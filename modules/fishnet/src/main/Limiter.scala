package lila.fishnet

import scala.concurrent.duration._
import reactivemongo.api.bson._

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

  private val maxPerDay = 30

  private def perDayCheck(sender: Work.Sender) =
    sender match {
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(Some(userId), _, _, _)              => requesterApi.countToday(userId) map (_ < maxPerDay)
      case Work.Sender(_, Some(ip), _, _) =>
        fuccess {
          RequestLimitPerIP(ip, cost = 1)(true)(false)
        }
      case _ => fuFalse
    }
}
