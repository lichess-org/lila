package lila.team

import lila.db.dsl._

final private[team] class TeamCli(
    teamRepo: TeamRepo,
    api: TeamApi
)(implicit ec: scala.concurrent.ExecutionContext)
    extends lila.common.Cli {

  import BSONHandlers._

  def process = {

    case "team" :: "enable" :: team :: Nil => perform(team)(api.enable)

    case "team" :: "recompute" :: "nbMembers" :: "all" :: Nil =>
      api.recomputeNbMembers
      fuccess("In progress... it will take a while")

    case "team" :: "recompute" :: "nbMembers" :: teamId :: Nil =>
      api.recomputeNbMembers(teamId) inject "done"
  }

  private def perform(teamId: String)(op: Team => Funit): Fu[String] =
    teamRepo.coll.byId[Team](teamId) flatMap {
      _.fold(fufail[String]("Team not found")) { u =>
        op(u) inject "Success"
      }
    }
}
