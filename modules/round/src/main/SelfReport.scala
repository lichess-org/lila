package lila.round

import com.softwaremill.tagging._
import scala.concurrent.duration._

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
    endGameSetting: SettingStore[Strings] @@ SelfReportEndGame,
    markUserSetting: SettingStore[Strings] @@ SelfReportMarkUser
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val onceEvery = lila.memo.OnceEvery(1 hour)

  def apply(
      userId: Option[User.ID],
      ip: IpAddress,
      fullId: Game.FullId,
      name: String
  ): Funit =
    userId ?? userRepo.named flatMap { user =>
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
      if (fullId.value == "____________") fuccess(doLog())
      else
        proxyRepo.pov(fullId.value) flatMap {
          _ ?? { pov =>
            if (!known) doLog()
            if (
              endGameSetting.get().value.has(name) ||
              (name.startsWith("soc") && (
                name.contains("stockfish") || name.contains("userscript") ||
                  name.contains("__puppeteer_evaluation_script__")
              ))
            ) fuccess {
              if (userId.isDefined) tellRound(pov.gameId, lila.round.actorApi.round.Cheat(pov.color))
              user.ifTrue(markUserSetting.get().value.has(name)) foreach { u =>
                lila.common.Bus.publish(lila.hub.actorApi.mod.SelfReportMark(u.id, name), "selfReportMark")
              }
            }
            else gameRepo.setBorderAlert(pov).void
          }
        }
    }
}
