package lila.fishnet

import scala.concurrent.duration._
import reactivemongo.bson._

import lila.db.dsl.Coll

private final class Limiter(
    analysisColl: Coll,
    requesterApi: lila.analyse.RequesterApi) {

  def apply(sender: Work.Sender): Fu[Boolean] =
    concurrentCheck(sender) flatMap {
      case false => fuccess(false)
      case true  => perDayCheck(sender)
    }

  private val RequestLimitPerIP = new lila.memo.RateLimit(
    credits = 50,
    duration = 20 hours,
    name = "request analysis per IP",
    key = "request_analysis.ip")

  private def concurrentCheck(sender: Work.Sender) = sender match {
    case Work.Sender(_, _, mod, system) if (mod || system) => fuccess(true)
    case Work.Sender(Some(userId), _, _, _) => analysisColl.count(BSONDocument(
      "sender.userId" -> userId
    ).some) map (0 ==)
    case Work.Sender(_, Some(ip), _, _) => analysisColl.count(BSONDocument(
      "sender.ip" -> ip
    ).some) map (0 ==)
    case _ => fuccess(false)
  }

  private def perDayCheck(sender: Work.Sender) = sender match {
    case Work.Sender(_, _, mod, system) if (mod || system) => fuccess(true)
    case Work.Sender(Some(userId), _, _, _)                => requesterApi.countToday(userId) map (_ < 25)
    case Work.Sender(_, Some(ip), _, _) => fuccess {
      RequestLimitPerIP(ip, cost = 1)(true)
    }
    case _ => fuccess(false)
  }
}
