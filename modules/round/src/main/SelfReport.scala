package lila.round

import com.softwaremill.tagging.*
import scalalib.ThreadLocalRandom
import scalalib.net.IpAddressStr
import scala.util.matching.Regex
import chess.IntRating

import lila.core.net.IpAddress
import lila.memo.SettingStore
import lila.rating.UserPerfsExt.bestRating
import lila.user.UserApi

final class SelfReport(
    roundApi: lila.core.round.RoundApi,
    userApi: UserApi,
    proxyRepo: GameProxyRepo,
    noteApi: lila.core.user.NoteApi,
    endGameSetting: SettingStore[Regex] @@ SelfReportEndGame,
    markUserSetting: SettingStore[Regex] @@ SelfReportMarkUser
)(using ec: Executor, scheduler: Scheduler):

  private val logOnceEvery = scalalib.cache.OnceEvery[IpAddressStr](1.minute)

  private val logger = lila.log("cheat").branch("jslog")

  def apply(userId: Option[UserId], ip: IpAddress, fullId: GameFullId, name: String): Funit =
    userId.so(userApi.withPerfs).flatMap { user =>
      val gameUrl = s"https://lichess.org/$fullId"
      if name != "ceval" && logOnceEvery(ip.str) then
        logger.info(s"$ip $gameUrl ${user.fold("anon")(_.id)} $name")
        lila.mon.cheat.selfReport(name, userId.isDefined).increment()
        funit
      else
        user.so: u =>
          proxyRepo
            .pov(fullId)
            .mapz: pov =>
              if name != "err" then noteApi.lichessWrite(u.user, s"Self-report $name on $gameUrl")
              if endGameSetting.get().matches(name) ||
                (name.startsWith("soc") && (
                  name.contains("stockfish") || name.contains("userscript") ||
                    name.contains("__puppeteer_evaluation_script__")
                ))
              then roundApi.tell(pov.gameId, lila.core.round.Cheat(pov.color))
              if markUserSetting.get().matches(name) then
                val rating = pov.player.rating | u.perfs.bestRating
                val delayBase =
                  if rating > IntRating(2500) then 2
                  else if rating > IntRating(2300) then 10
                  else if rating > IntRating(2000) then 30
                  else if rating > IntRating(1800) then 60
                  else 120
                val delay = delayBase.minutes + ThreadLocalRandom.nextInt(delayBase * 60).seconds
                scheduler.scheduleOnce(delay):
                  lila.common.Bus.pub(lila.core.mod.SelfReportMark(u.id, name, fullId))
    }
