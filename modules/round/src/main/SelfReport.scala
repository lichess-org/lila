package lila.round

import lila.common.IpAddress
import lila.user.{ User, UserRepo }
import lila.hub.DuctMap

final class SelfReport(roundMap: DuctMap[Round]) {

  def apply(
    userId: Option[User.ID],
    ip: IpAddress,
    fullId: String,
    name: String
  ): Funit =
    userId.??(UserRepo.named) flatMap { user =>
      val known = user.??(_.engine)
      lila.mon.cheat.cssBot()
      // user.ifTrue(!known && name != "ceval") ?? { u =>
      //   Env.report.api.autoBotReport(u.id, referer, name)
      // }
      def doLog = lila.log("cheat").branch("jslog").info(
        s"$ip https://lichess.org/$fullId ${user.fold("anon")(_.id)} $name"
      )
      lila.game.GameRepo pov fullId map {
        _ ?? { pov =>
          if (!known) doLog
          if (Set("ceval", "rcb", "ccs")(name)) fuccess {
            roundMap.tell(
              pov.gameId,
              lila.round.actorApi.round.Cheat(pov.color)
            )
          }
          else lila.game.GameRepo.setBorderAlert(pov)
        }
      }
    }
}
