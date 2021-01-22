package lila.tournament

import play.api.data._

import lila.hub.LightTeam.TeamID
import lila.user.User

case class TeamBattle(
    teams: Set[TeamID],
    nbLeaders: Int
) {
  def hasEnoughTeams     = teams.sizeIs > 1
  lazy val sortedTeamIds = teams.toList.sorted
}

object TeamBattle {

  val maxTeams     = 1000
  val displayTeams = 10

  def init(teamId: TeamID) = TeamBattle(Set(teamId), 5)

  case class TeamVs(teams: chess.Color.Map[TeamID])

  class RankedTeam(
      val rank: Int,
      val teamId: TeamID,
      val leaders: List[TeamLeader],
      val score: Int
  ) extends Ordered[RankedTeam] {
    private def magicScore = leaders.foldLeft(0)(_ + _.magicScore)
    def this(rank: Int, teamId: TeamID, leaders: List[TeamLeader]) =
      this(rank, teamId, leaders, leaders.foldLeft(0)(_ + _.score))
    def updateRank(newRank: Int) = new RankedTeam(newRank, teamId, leaders, score)
    override def compare(that: RankedTeam) = {
      if (this.score > that.score) -1
      else if (this.score < that.score) 1
      else that.magicScore - this.magicScore
    }
  }

  case class TeamLeader(userId: User.ID, magicScore: Int) {
    def score: Int = magicScore / 10000
  }

  case class TeamInfo(
      teamId: TeamID,
      nbPlayers: Int,
      avgRating: Int,
      avgPerf: Int,
      avgScore: Int,
      topPlayers: List[Player]
  )

  object DataForm {
    import play.api.data.Forms._

    val fields = mapping(
      "teams"     -> nonEmptyText,
      "nbLeaders" -> number(min = 1, max = 20)
    )(Setup.apply)(Setup.unapply)
      .verifying("We need at least 2 teams", s => s.potentialTeamIds.sizeIs > 1)
      .verifying(
        s"In this version of team battles, no more than $maxTeams teams can be allowed.",
        s => s.potentialTeamIds.sizeIs <= maxTeams
      )

    def edit(teams: List[String], nbLeaders: Int) =
      Form(fields) fill
        Setup(s"${teams mkString "\n"}\n", nbLeaders)

    def empty = Form(fields)

    case class Setup(
        teams: String,
        nbLeaders: Int
    ) {
      def potentialTeamIds: Set[TeamID] =
        teams.linesIterator.map(_.takeWhile(' ' !=)).filter(_.nonEmpty).toSet
    }
  }
}
