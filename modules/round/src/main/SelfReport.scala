package lila.round

import scala.concurrent.duration._

import lila.common.IpAddress
import lila.game.{ Game, Pov }
import lila.hub.DuctMap
import lila.user.{ User, UserRepo }

final class SelfReport(
    tellRound: TellRound,
    slackApi: lila.slack.SlackApi,
    proxyPov: String => Fu[Option[Pov]]
) {

  private val whitelist = Set("treehugger")

  private object recent {
    private val cache = new lila.memo.ExpireSetMemo(10 minutes)
    def isNew(user: User, fullId: Game.FullId): Boolean = {
      val key = s"${user.id}:${fullId}"
      val res = !cache.get(key)
      cache.put(key)
      res
    }
  }

  def apply(
    userId: Option[User.ID],
    ip: IpAddress,
    fullId: Game.FullId,
    name: String
  ): Funit = !userId.exists(whitelist.contains) ?? {
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
        user.filter(recent.isNew(_, fullId)) ?? { u =>
          slackApi.selfReport(
            typ = name,
            path = fullId.value,
            user = u,
            ip = ip
          )
        }
      }
      if (fullId == "________") fuccess(doLog)
      else proxyPov(fullId.value) map {
        _ ?? { pov =>
          if (!known) doLog
          if (Set("ceval", "rcb", "ccs")(name)) fuccess {
            tellRound(
              pov.gameId,
              lila.round.actorApi.round.Cheat(pov.color)
            )
          }
          else lila.game.GameRepo.setBorderAlert(pov).void
        }
      }
    }
  }
}
