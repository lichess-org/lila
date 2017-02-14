package lila.team

import lila.db.dsl._
import lila.user.UserRepo

private[team] final class Cli(api: TeamApi, coll: Colls) extends lila.common.Cli {

  import BSONHandlers._

  def process = {

    case "team" :: "join" :: team :: users => perform(team, users)(api.doJoin)

    case "team" :: "quit" :: team :: users => perform(team, users)(api.doQuit)

    case "team" :: "enable" :: team :: Nil => perform(team)(api.enable)

    case "team" :: "disable" :: team :: Nil => perform(team)(api.disable)

    case "team" :: "recompute" :: "nbMembers" :: Nil =>
      api.recomputeNbMembers inject "done"
  }

  private def perform(teamId: String)(op: Team => Funit): Fu[String] =
    coll.team.byId[Team](teamId) flatMap {
      _.fold(fufail[String]("Team not found")) { u => op(u) inject "Success" }
    }

  private def perform(teamId: String, userIds: List[String])(op: (Team, String) => Funit): Fu[String] =
    coll.team.byId[Team](teamId) flatMap {
      _.fold(fufail[String]("Team not found")) { team =>
        UserRepo nameds userIds flatMap { users =>
          users.map(user => {
            logger.info(user.username)
            op(team, user.id)
          }).sequenceFu
        } inject "Success"
      }
    }
}
