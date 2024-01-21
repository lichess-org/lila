package lila.round

import lila.common.IpAddress
import lila.game.Game
import lila.user.{ User, UserRepo }

final class SelfReport(
    tellRound: TellRound,
    gameRepo: lila.game.GameRepo,
    userRepo: UserRepo,
    proxyRepo: GameProxyRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val whitelist = Set("treehugger")

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
                s"$ip https://lishogi.org/$fullId ${user.fold("anon")(_.id)} $name"
              )
          }
        if (fullId.value == "________") fuccess(doLog())
        else
          proxyRepo.pov(fullId.value) flatMap {
            _ ?? { pov =>
              if (!known) doLog()
              if (Set("ceval", "rcb", "ccs")(name)) fuccess {
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
