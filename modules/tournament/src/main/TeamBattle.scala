package lila.tournament

import play.api.data._
import play.api.data.Forms._

import lila.hub.lightTeam._

case class TeamBattle(
    teams: Set[TeamId]
) {
  lazy val sortedTeamIds = teams.toList.sorted
}

object TeamBattle {

  case class RankedTeamId(rank: Int, teamId: TeamId, magicScore: Int) {
    def score: Int = magicScore / 10000
  }

  object DataForm {
    import play.api.data.Forms._
    import lila.common.Form._

    def edit(teams: List[String]) = Form(fields) fill Setup(s"${teams mkString "\n"}\n")

    val fields = mapping(
      "teams" -> nonEmptyText
    )(Setup.apply)(Setup.unapply)
      .verifying("We need at least 2 teams", s => s.potentialTeamIds.size > 1)

    case class Setup(
        teams: String
    ) {
      def potentialTeamIds: Set[String] =
        teams.lines.map(_.takeWhile(' ' !=)).filter(_.nonEmpty).toSet
    }
  }
}
