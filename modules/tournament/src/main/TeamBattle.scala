package lila.tournament

import play.api.data.*

case class TeamBattle(
    teams: Set[TeamId],
    nbLeaders: Int
):
  def hasEnoughTeams             = teams.sizeIs > 1
  private given Ordering[TeamId] = stringOrdering
  lazy val sortedTeamIds         = teams.toList.sorted

  def hasTooManyTeams = teams.sizeIs > TeamBattle.displayTeams

object TeamBattle:

  val maxTeams     = 200
  val displayTeams = 10

  val blacklist: Set[TeamId] =
    Set("lichess-swiss", "lichess-curator", "lichess-productions", "lichess-broadcasts").map { TeamId(_) }

  def init(teamId: TeamId) = TeamBattle(Set(teamId), 5)

  case class TeamVs(teams: chess.ByColor[TeamId])

  class RankedTeam(
      val rank: Int,
      val teamId: TeamId,
      val leaders: List[TeamLeader],
      val score: Int
  ) extends Ordered[RankedTeam]:
    private def magicScore = leaders.foldLeft(0)(_ + _.magicScore)
    def this(rank: Int, teamId: TeamId, leaders: List[TeamLeader]) =
      this(rank, teamId, leaders, leaders.foldLeft(0)(_ + _.score))
    def updateRank(newRank: Int) = new RankedTeam(newRank, teamId, leaders, score)
    override def compare(that: RankedTeam) =
      if this.score > that.score then -1
      else if this.score < that.score then 1
      else that.magicScore - this.magicScore

  case class TeamLeader(userId: UserId, magicScore: Int):
    def score: Int = magicScore / 10000

  case class TeamInfo(
      teamId: TeamId,
      nbPlayers: Int,
      avgRating: Int,
      avgPerf: Int,
      avgScore: Int,
      topPlayers: List[Player]
  )

  object DataForm:
    import play.api.data.Forms.*

    val fields = mapping(
      "teams"     -> nonEmptyText,
      "nbLeaders" -> number(min = 1, max = 20)
    )(Setup.apply)(lila.common.unapply)
      .verifying("We need at least 2 teams", s => s.potentialTeamIds.sizeIs > 1)
      .verifying(
        s"In this version of team battles, no more than $maxTeams teams can be allowed.",
        s => s.potentialTeamIds.sizeIs <= maxTeams
      )

    def edit(teams: List[String], nbLeaders: Int) =
      Form(fields).fill(Setup(s"${teams.mkString("\n")}\n", nbLeaders))

    def empty = Form(fields)

    case class Setup(teams: String, nbLeaders: Int):
      // guess if newline or comma separated
      def potentialTeamIds: Set[TeamId] =
        val lines = teams.linesIterator.toList
        val dirtyIds =
          if lines.sizeIs > 1 then lines.map(_.takeWhile(' ' !=))
          else lines.headOption.so(_.split(',').toList)
        dirtyIds.map(_.trim).filter(_.nonEmpty).map(TeamId(_)).toSet
