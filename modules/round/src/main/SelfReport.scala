package lila.round

import com.softwaremill.tagging.*
import scala.concurrent.duration.*
import scala.util.matching.Regex
import ornicar.scalalib.ThreadLocalRandom

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
)(using ec: scala.concurrent.ExecutionContext, scheduler: akka.actor.Scheduler):

  private val onceEvery = lila.memo.OnceEvery[UserId](1 hour)

  def apply(
      userId: Option[UserId],
      ip: IpAddress,
      fullId: GameFullId,
      name: String
  ): Funit =
    userId ?? userRepo.byId map { user =>
      val known = user.exists(_.marks.engine)
      // user.ifTrue(!known && name != "ceval") ?? { u =>
      //   Env.report.api.autoBotReport(u.id, referer, name)
      // }
      def doLog(): Unit =
        if (name != "ceval")
          lila.log("cheat").branch("jslog").info {
            s"$ip https://lichess.org/$fullId ${user.fold("anon")(_.id)} $name"
          }
          user.filter(u => onceEvery(u.id)) foreach { u =>
            lila.mon.cheat.selfReport(name, userId.isDefined).increment()
            ircApi.selfReport(
              typ = name,
              path = fullId.value,
              user = u,
              ip = ip
            )
          }
      if (fullId.value == "____________") doLog()
      else
        proxyRepo.pov(fullId) foreach {
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
                scheduler.scheduleOnce(
                  (10 + ThreadLocalRandom.nextInt(24 * 60)).minutes
                ) {
                  lila.common.Bus.publish(lila.hub.actorApi.mod.SelfReportMark(u.id, name), "selfReportMark")
                }
            }
          }
        }
    }
