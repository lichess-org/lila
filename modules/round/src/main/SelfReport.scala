package lidraughts.round

import akka.actor._

import lidraughts.common.IpAddress
import lidraughts.user.{ User, UserRepo }

final class SelfReport(roundMap: ActorRef) {

  def apply(
    userId: Option[User.ID],
    ip: IpAddress,
    fullId: String,
    name: String
  ): Funit =
    userId.??(UserRepo.named) flatMap { user =>
      val known = user.??(_.engine)
      lidraughts.mon.cheat.cssBot()
      // user.ifTrue(!known && name != "ceval") ?? { u =>
      //   Env.report.api.autoBotReport(u.id, referer, name)
      // }
      def doLog = lidraughts.log("cheat").branch("jslog").info(
        s"$ip https://lidraughts.org/$fullId ${user.fold("anon")(_.id)} $name"
      )
      lidraughts.game.GameRepo pov fullId map {
        _ ?? { pov =>
          if (!known) doLog
          if (Set("ceval", "rcb", "ccs")(name)) fuccess {
            roundMap ! lidraughts.hub.actorApi.map.Tell(
              pov.gameId,
              lidraughts.round.actorApi.round.Cheat(pov.color)
            )
          }
          else lidraughts.game.GameRepo.setBorderAlert(pov)
        }
      }
    }
}
