package lila.round

import com.softwaremill.tagging._
import scala.concurrent.duration._
import scala.util.matching.Regex

import lila.common.{ IpAddress, Strings }
import lila.game.Game
import lila.memo.SettingStore
import lila.user.{ User, UserRepo }

final class SelfReport(
    tellRound: TellRound,
    gameRepo: lila.game.GameRepo,
    userRepo: UserRepo,
    ircApi: lila.irc.IrcApi,
    proxyRepo: GameProxyRepo,
    endGameSetting: SettingStore[Regex] @@ SelfReportEndGame,
    markUserSetting: SettingStore[Regex] @@ SelfReportMarkUser
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val onceEvery = lila.memo.OnceEvery(1 hour)

  def apply(
      userId: Option[User.ID],
      ip: IpAddress,
      fullId: Game.FullId,
      name: String
  ): Funit =
    userId ?? userRepo.named map { user =>
      val known = user.exists(_.marks.engine)
      lila.mon.cheat.cssBot.increment()
      // user.ifTrue(!known && name != "ceval") ?? { u =>
      //   Env.report.api.autoBotReport(u.id, referer, name)
      // }
      def doLog(): Unit =
        if (name != "ceval") {
          lila.log("cheat").branch("jslog").info {
            s"$ip https://lichess.org/$fullId ${user.fold("anon")(_.id)} $name"
          }
          user.filter(u => onceEvery(u.id)) foreach { u =>
            ircApi.selfReport(
              typ = name,
              path = fullId.value,
              user = u,
              ip = ip
            )
          }
        }
      if (fullId.value == "____________") doLog()
      else
        proxyRepo.pov(fullId.value) foreach {
          _ ?? { pov =>
            if (!known) doLog()
            user foreach { u =>
              if (
                endGameSetting.get().matches(name) ||
                (name.startsWith("soc") && (
                  name.contains("stockfish") || name.contains("userscript") ||
                    name.contains("__puppeteer_evaluation_script__")
                ))
              ) tellRound(pov.gameId, lila.round.actorApi.round.Cheat(pov.color))
              if (markUserSetting.get().matches(name))
                lila.common.Bus.publish(lila.hub.actorApi.mod.SelfReportMark(u.id, name), "selfReportMark")
            }
          }
        }
    }
}
