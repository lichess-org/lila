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
    (name != "err").so:
      val gameUrl = s"https://lichess.org/$fullId"
      if name != "ceval" && logOnceEvery(ip.str) then
        logger.info(s"$ip $gameUrl ${userId | "-"} $name")
        lila.mon.cheat.selfReport(name, userId.isDefined).increment()
      userId.so(userApi.withPerfs).flatMapz { u =>
        proxyRepo
          .pov(fullId)
          .mapz: pov =>
            if endGameSetting.get().matches(name) ||
              (name.startsWith("soc") && (
                name.contains("stockfish") || name.contains("userscript") ||
                  name.contains("__puppeteer_evaluation_script__")
              ))
            then roundApi.tell(pov.gameId, lila.core.round.Cheat(pov.color))
            val banDelay = markUserSetting
              .get()
              .matches(name)
              .option:
                val rating = pov.player.rating | u.perfs.bestRating
                val delayBase =
                  if rating > IntRating(2500) then 2
                  else if rating > IntRating(2300) then 10
                  else if rating > IntRating(2000) then 30
                  else if rating > IntRating(1800) then 60
                  else 120
                delayBase.minutes + ThreadLocalRandom.nextInt(delayBase * 60).seconds
            val msg = s"Self-report $name on $gameUrl, " +
              banDelay.fold("no ban")(d => s"ban in ${d.toMinutes} minutes")
            noteApi.lichessWrite(u.user, msg)
            banDelay.foreach: d =>
              scheduler.scheduleOnce(d):
                lila.common.Bus.pub(lila.core.mod.SelfReportMark(u.id, name, fullId))
      }
