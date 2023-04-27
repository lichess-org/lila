package lila.gathering

import play.api.data.Forms.*

import lila.rating.PerfType
import play.api.i18n.Lang
import lila.rating.Perf
import lila.hub.LeaderTeam
import lila.hub.LightTeam.TeamName
import lila.gathering.Condition.*
import lila.common.Form.{ *, given }

object ConditionForm:

  val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
  val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}").map:
    case (0, _) => (0, "No restriction")
    case x      => x

  val nbRatedGame = mapping(
    "nb" -> numberIn(nbRatedGameChoices)
  )(NbRatedGame.apply)(_.nb.some)

  val maxRatings =
    List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)

  val maxRatingChoices = ("", "No restriction") ::
    options(maxRatings, "Max rating of %d").toList.map { (k, v) => k.toString -> v }

  val maxRating = mapping(
    "rating" -> numberIn(maxRatings).into[IntRating]
  )(MaxRating.apply)(_.rating.some)

  val minRatings =
    List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600)

  val minRatingChoices = ("", "No restriction") ::
    options(minRatings, "Min rating of %d").toList.map { (k, v) => k.toString -> v }

  val minRating = mapping(
    "rating" -> numberIn(minRatings).into[IntRating]
  )(MinRating.apply)(_.rating.some)

  def teamMember(leaderTeams: List[LeaderTeam]) =
    mapping(
      "teamId" -> optional(of[TeamId].verifying(id => leaderTeams.exists(_.id == id)))
    )(TeamMemberSetup.apply)(_.teamId.some)

  case class TeamMemberSetup(teamId: Option[TeamId]):
    def convert(teams: Map[TeamId, TeamName]): Option[TeamMember] =
      teamId flatMap { id =>
        teams.get(id) map { TeamMember(id, _) }
      }

  object TeamMemberSetup:
    def apply(x: TeamMember): TeamMemberSetup = TeamMemberSetup(x.teamId.some)
