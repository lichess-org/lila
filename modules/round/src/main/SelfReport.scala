package lidraughts.round

import lidraughts.common.IpAddress
import lidraughts.game.Pov
import lidraughts.hub.DuctMap
import lidraughts.user.{ User, UserRepo }

final class SelfReport(roundMap: DuctMap[RoundDuct], proxyPov: String => Fu[Option[Pov]]) {

  private val whitelist = Set("")

  def apply(
    userId: Option[User.ID],
    ip: IpAddress,
    fullId: String,
    name: String
  ): Funit = !userId.exists(whitelist.contains) ?? {
    userId.??(UserRepo.named) flatMap { user =>
      val known = user.??(_.engine)
      lidraughts.mon.cheat.cssBot()
      // user.ifTrue(!known && name != "ceval") ?? { u =>
      //   Env.report.api.autoBotReport(u.id, referer, name)
      // }
      def doLog = lidraughts.log("cheat").branch("jslog").info(
        s"$ip https://lidraughts.org/$fullId ${user.fold("anon")(_.id)} $name"
      )
      if (fullId == "________") fuccess(doLog)
      else proxyPov(fullId) map {
        _ ?? { pov =>
          if (!known) doLog
          if (Set("ceval", "rcb", "ccs")(name)) fuccess {
            roundMap.tell(
              pov.gameId,
              lidraughts.round.actorApi.round.Cheat(pov.color)
            )
          }
          else lidraughts.game.GameRepo.setBorderAlert(pov)
        }
      }
    }
  }
}
