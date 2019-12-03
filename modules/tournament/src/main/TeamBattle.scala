package lila.tournament

import play.api.data._
import play.api.data.Forms._

import lila.hub.lightTeam._
import lila.user.User

case class TeamBattle(
    teams: Set[TeamId],
    nbLeaders: Int
) {
  def hasEnoughTeams = teams.size > 1
  lazy val sortedTeamIds = teams.toList.sorted
}

object TeamBattle {

  def init(teamId: TeamId) = TeamBattle(Set(teamId), 5)

  case class RankedTeam(
      rank: Int,
      teamId: TeamId,
      leaders: List[TeamLeader]
  ) {
    def magicScore = leaders.foldLeft(0)(_ + _.magicScore)
    def score = leaders.foldLeft(0)(_ + _.score)
  }

  case class TeamLeader(userId: User.ID, magicScore: Int) {
    def score: Int = magicScore / 10000
  }

  case class TeamInfo(
      teamId: TeamId,
      nbPlayers: Int,
      avgRating: Int,
      avgPerf: Int,
      avgScore: Int,
      topPlayers: List[Player]
  )

  object DataForm {
    import play.api.data.Forms._
    import lila.common.Form._

    val fields = mapping(
      "teams" -> nonEmptyText,
      "nbLeaders" -> number(min = 1, max = 10)
    )(Setup.apply)(Setup.unapply)
      .verifying("We need at least 2 teams", s => s.potentialTeamIds.size > 1)
      .verifying("In this version of team battles, no more than 10 teams can be allowed.", s => s.potentialTeamIds.size <= 10)

    def edit(teams: List[String], nbLeaders: Int) = Form(fields) fill
      Setup(s"${teams mkString "\n"}\n", nbLeaders)

    def empty = Form(fields)

    case class Setup(
        teams: String,
        nbLeaders: Int
    ) {
      def potentialTeamIds: Set[String] =
        teams.linesIterator.map(_.takeWhile(' ' !=)).filter(_.nonEmpty).toSet
    }
  }
}
