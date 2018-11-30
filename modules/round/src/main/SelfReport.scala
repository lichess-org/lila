package lila.round

import lila.common.IpAddress
import lila.hub.DuctMap
import lila.user.{ User, UserRepo }

final class SelfReport(
    roundMap: DuctMap[Round],
    slackApi: lila.slack.SlackApi
) {

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
      def doLog = if (name != "ceval") {
        lila.log("cheat").branch("jslog").info(
          s"$ip https://lichess.org/$fullId ${user.fold("anon")(_.id)} $name"
        )
        user ?? { u =>
          slackApi.selfReport(
            typ = name,
            path = fullId,
            user = u,
            ip = ip
          )
        }
      }
      if (fullId == "________") fuccess(doLog)
      else lila.game.GameRepo pov fullId map {
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
