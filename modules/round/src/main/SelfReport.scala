package lila.round

import com.softwaremill.tagging.*
import scalalib.ThreadLocalRandom
import scala.util.matching.Regex
import chess.IntRating

import lila.core.net.{ IpAddress, IpAddressStr }
import lila.memo.SettingStore
import lila.rating.UserPerfsExt.bestRating
import lila.user.UserApi

final class SelfReport(
    roundApi: lila.core.round.RoundApi,
    userApi: UserApi,
    proxyRepo: GameProxyRepo,
    endGameSetting: SettingStore[Regex] @@ SelfReportEndGame,
    markUserSetting: SettingStore[Regex] @@ SelfReportMarkUser
)(using ec: Executor, scheduler: Scheduler):

  private val logOnceEvery = scalalib.cache.OnceEvery[IpAddressStr](1.minute)

  def apply(
      userId: Option[UserId],
      ip: IpAddress,
      fullId: GameFullId,
      name: String
  ): Funit =
    userId.so(userApi.withPerfs).map { user =>
      val known = user.exists(_.marks.engine)
      // user.ifTrue(!known && name != "ceval") so { u =>
      //   Env.report.api.autoBotReport(u.id, referer, name)
      // }
      def doLog(): Unit =
        if name != "ceval" && logOnceEvery(ip.str) then
          lila
            .log("cheat")
            .branch("jslog")
            .info:
              s"$ip https://lichess.org/$fullId ${user.fold("anon")(_.id)} $name"
          lila.mon.cheat.selfReport(name, userId.isDefined).increment()
      if fullId.value == "____________" then doLog()
      else
        proxyRepo
          .pov(fullId)
          .foreach:
            _.so: pov =>
              if !known then doLog()
              user.foreach: u =>
                if endGameSetting.get().matches(name) ||
                  (name.startsWith("soc") && (
                    name.contains("stockfish") || name.contains("userscript") ||
                      name.contains("__puppeteer_evaluation_script__")
                  ))
                then roundApi.tell(pov.gameId, lila.core.round.Cheat(pov.color))
                if markUserSetting.get().matches(name) then
                  val rating = u.perfs.bestRating
                  val delayBase =
                    if rating > IntRating(2500) then 0
                    else if rating > IntRating(2300) then 1
                    else if rating > IntRating(2000) then 6
                    else if rating > IntRating(1800) then 12
                    else 24
                  val delay = (2 + delayBase + ThreadLocalRandom.nextInt(delayBase * 60)).minutes
                  scheduler.scheduleOnce(delay):
                    lila.common.Bus.publish(lila.core.mod.SelfReportMark(u.id, name), "selfReportMark")
    }
