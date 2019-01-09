package lila.round

import scala.concurrent.duration._

import lila.common.IpAddress
import lila.hub.DuctMap
import lila.user.{ User, UserRepo }

final class SelfReport(
    roundMap: DuctMap[Round],
    slackApi: lila.slack.SlackApi
) {

  private val whitelist = Set("treehugger")

  private object recent {
    private val cache = new lila.memo.ExpireSetMemo(10 minutes)
    def isNew(user: User, fullId: String): Boolean = {
      val key = s"${user.id}:${fullId}"
      val res = !cache.get(key)
      cache.put(key)
      res
    }
  }

  def apply(
    userId: Option[User.ID],
    ip: IpAddress,
    fullId: String,
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
}
