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

  val perfAuto = "auto" -> "Auto"
  val perfKeys = "auto" :: PerfType.nonPuzzle.map(_.key)
  def perfChoices(using Lang) =
    perfAuto :: PerfType.nonPuzzle.map { pt =>
      pt.key -> pt.trans
    }

  val nbRatedGames = Seq(0, 5, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200)
  val nbRatedGameChoices = options(nbRatedGames, "%d rated game{s}").map:
    case (0, _) => (0, "No restriction")
    case x      => x

  val nbRatedGame = mapping(
    "perf" -> optional(of[Perf.Key].verifying(perfKeys.contains)),
    "nb"   -> numberIn(nbRatedGameChoices)
  )(NbRatedGameSetup.apply)(unapply)

  case class NbRatedGameSetup(perf: Option[Perf.Key], nb: Int):
    def convert(tourPerf: PerfType): Option[NbRatedGame] =
      nb > 0 option NbRatedGame(
        if (perf has perfAuto._1) tourPerf.some
        else perf.flatMap(PerfType.apply),
        nb
      )

  object NbRatedGameSetup:
    def apply(x: NbRatedGame): NbRatedGameSetup = NbRatedGameSetup(x.perf.map(_.key), x.nb)

  case class RatingSetup(perf: Option[Perf.Key], rating: Option[IntRating]):
    def actualRating = rating.filter(r => r > 600 && r < 3000)
    def convert[A](gatheringPerf: PerfType)(f: (PerfType, IntRating) => A): Option[A] =
      actualRating map { r =>
        f(perf.flatMap(PerfType.apply) | gatheringPerf, r)
      }

  object RatingSetup:
    def apply(v: (Option[PerfType], Option[IntRating])): RatingSetup = RatingSetup(v._1.map(_.key), v._2)

  val maxRatings =
    List(2200, 2100, 2000, 1900, 1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900, 800)

  val maxRatingChoices = ("", "No restriction") ::
    options(maxRatings, "Max rating of %d").toList.map { (k, v) => k.toString -> v }

  val maxRating = mapping(
    "perf"   -> optional(of[Perf.Key].verifying(perfKeys.contains)),
    "rating" -> optional(numberIn(maxRatings).into[IntRating])
  )(RatingSetup.apply)(unapply)

  val minRatings =
    List(1000, 1100, 1200, 1300, 1400, 1500, 1600, 1700, 1800, 1900, 2000, 2100, 2200, 2300, 2400, 2500, 2600)

  val minRatingChoices = ("", "No restriction") ::
    options(minRatings, "Min rating of %d").toList.map { (k, v) => k.toString -> v }

  val minRating = mapping(
    "perf"   -> optional(of[Perf.Key].verifying(perfKeys.contains)),
    "rating" -> optional(numberIn(minRatings).into[IntRating])
  )(RatingSetup.apply)(unapply)

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
