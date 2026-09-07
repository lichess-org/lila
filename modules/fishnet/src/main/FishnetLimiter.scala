package lila.fishnet

import lila.core.net.IpAddress
import lila.db.dsl.{ *, given }

final private class FishnetLimiter(
    analysisColl: Coll,
    requesterApi: lila.analyse.RequesterApi
)(using Executor, lila.core.config.RateLimit):

  import FishnetLimiter.*

  def apply(sender: Work.Sender, ignoreConcurrentCheck: Boolean, ownGame: Boolean): Fu[Analyser.Result] =
    (fuccess(ignoreConcurrentCheck) >>| concurrentCheck(sender))
      .flatMap:
        if _ then perDayCheck(sender)
        else fuccess(Analyser.Result.ConcurrentAnalysis)
      .flatMap: result =>
        (result.ok.so(requesterApi.add(sender.userId, ownGame))).inject(result)

  private val RequestLimitPerIP = lila.memo.RateLimit[IpAddress](
    credits = 120,
    duration = 1.day,
    key = "request_analysis.ip"
  )

  private def concurrentCheck(sender: Work.Sender) =
    sender match
      case Work.Sender(_, _, mod, system) if mod || system => fuTrue
      case Work.Sender(userId, ip, _, _) =>
        analysisColl
          .exists(
            $or(
              $doc("sender.ip" -> ip),
              $doc("sender.userId" -> userId)
            )
          )
          .not

  private def perDayCheck(sender: Work.Sender): Fu[Analyser.Result] =
    sender match
      case Work.Sender(_, _, mod, system) if mod || system => fuccess(Analyser.Result.Ok)
      case Work.Sender(userId, ip, _, _) =>
        def perUser = requesterApi
          .countTodayAndThisWeek(userId)
          .map: (daily, weekly) =>
            if weekly >= maxPerWeek then Analyser.Result.WeeklyLimit
            else if daily >= (if weekly < maxPerWeek * 2 / 3 then maxPerDay else maxPerDay * 2 / 3)
            then Analyser.Result.DailyLimit
            else Analyser.Result.Ok
        ip.fold(perUser): ipAddress =>
          RequestLimitPerIP(ipAddress, fuccess(Analyser.Result.DailyIpLimit))(perUser)

object FishnetLimiter:
  val maxPerDay = 40
  val maxPerWeek = 200
