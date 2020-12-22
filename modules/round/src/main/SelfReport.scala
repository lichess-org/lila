package lila.round

import scala.concurrent.duration._

import lila.common.IpAddress
import lila.game.Game
import lila.user.{ User, UserRepo }

final class SelfReport(
    tellRound: TellRound,
    gameRepo: lila.game.GameRepo,
    userRepo: UserRepo,
    slackApi: lila.slack.SlackApi,
    proxyRepo: GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val whitelist = Set("treehugger")

  private val onceEvery = lila.memo.OnceEvery(1 hour)

  def apply(
      userId: Option[User.ID],
      ip: IpAddress,
      fullId: Game.FullId,
      name: String
  ): Funit =
    !userId.exists(whitelist.contains) ?? {
      userId.??(userRepo.named) flatMap { user =>
        val known = user.exists(_.marks.engine)
        lila.mon.cheat.cssBot.increment()
        // user.ifTrue(!known && name != "ceval") ?? { u =>
        //   Env.report.api.autoBotReport(u.id, referer, name)
        // }
        def doLog(): Unit =
          if (name != "ceval") {
            lila
              .log("cheat")
              .branch("jslog")
              .info(
                s"$ip https://lichess.org/$fullId ${user.fold("anon")(_.id)} $name"
              )
            user.filter(u => onceEvery(u.id)) foreach { u =>
              slackApi.selfReport(
                typ = name,
                path = fullId.value,
                user = u,
                ip = ip
              )
            }
          }
        if (name == "kb" || fullId.value == "________") fuccess(doLog())
        else
          proxyRepo.pov(fullId.value) flatMap {
            _ ?? { pov =>
              if (!known) doLog()
              if (
                Set("ceval", "rcb", "cma", "lga")(name) ||
                (name.startsWith("soc") && (
                  name.contains("stockfish") || name.contains("userscript") ||
                    name.contains("__puppeteer_evaluation_script__")
                ))
              ) fuccess {
                tellRound(
                  pov.gameId,
                  lila.round.actorApi.round.Cheat(pov.color)
                )
              }
              else gameRepo.setBorderAlert(pov).void
            }
          }
      }
    }
}
