package lila.tournament

import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import lila.user.User
import lila.history.HistoryApi
import lila.hub.LeaderTeam
import lila.rating.PerfType
import lila.hub.LightTeam.TeamName
import alleycats.Zero

object TournamentCondition:

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      teamMember: Option[TeamMember],
      allowList: Option[AllowList]
  ) extends ConditionList(List(nbRatedGame, maxRating, minRating, titled, teamMember, allowList)):

    def withVerdicts(
        getMaxRating: GetMaxRating
    )(user: User, getUserTeamIds: User => Fu[List[TeamId]])(using Executor): Fu[WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user))
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
      }.parallel dmap WithVerdicts.apply

    def withRejoinVerdicts(user: User, getUserTeamIds: User => Fu[List[TeamId]])(using
        Executor
    ): Fu[WithVerdicts] =
      list.map {
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
        case c             => fuccess(WithVerdict(c, Accepted))
      }.parallel dmap WithVerdicts.apply

    def sameMaxRating(other: All) = maxRating.map(_.rating) == other.maxRating.map(_.rating)
    def sameMinRating(other: All) = minRating.map(_.rating) == other.minRating.map(_.rating)
    def sameRatings(other: All)   = sameMaxRating(other) && sameMinRating(other)

    def similar(other: All) = sameRatings(other) && titled == other.titled && teamMember == other.teamMember

  object All:
    val empty = All(
      nbRatedGame = none,
      maxRating = none,
      minRating = none,
      titled = none,
      teamMember = none,
      allowList = none
    )
    given zero: Zero[All] = Zero(empty)

  object form:
    import play.api.data.Forms.*
    import lila.common.Form.{ *, given }
    import lila.gathering.ConditionForm.*
    def all(leaderTeams: List[LeaderTeam]) =
      mapping(
        "nbRatedGame" -> optional(nbRatedGame),
        "maxRating"   -> maxRating,
        "minRating"   -> minRating,
        "titled"      -> optional(boolean),
        "teamMember"  -> optional(teamMember(leaderTeams)),
        "allowList"   -> optional(allowList)
      )(AllSetup.apply)(unapply).verifying("Invalid ratings", _.validRatings)

    case class AllSetup(
        nbRatedGame: Option[NbRatedGameSetup],
        maxRating: RatingSetup,
        minRating: RatingSetup,
        titled: Option[Boolean],
        teamMember: Option[TeamMemberSetup],
        allowList: Option[String]
    ):

      def validRatings =
        (minRating.actualRating, maxRating.actualRating) match
          case (Some(min), Some(max)) => min < max
          case _                      => true

      def convert(perf: PerfType, teams: Map[TeamId, TeamName]) =
        All(
          nbRatedGame.flatMap(_ convert perf),
          maxRating.convert(perf)(MaxRating.apply),
          minRating.convert(perf)(MinRating.apply),
          ~titled option Titled,
          teamMember.flatMap(_ convert teams),
          allowList = allowList map AllowList.apply
        )

    object AllSetup:
      val default = AllSetup(
        nbRatedGame = none,
        maxRating = RatingSetup(none, none),
        minRating = RatingSetup(none, none),
        titled = none,
        teamMember = none,
        allowList = none
      )
      def apply(all: All): AllSetup =
        AllSetup(
          nbRatedGame = all.nbRatedGame.map(NbRatedGameSetup.apply),
          maxRating = RatingSetup(all.maxRating.map(_.perf.key), all.maxRating.map(_.rating)),
          minRating = RatingSetup(all.minRating.map(_.perf.key), all.minRating.map(_.rating)),
          titled = all.titled has Titled option true,
          teamMember = all.teamMember.map(TeamMemberSetup.apply),
          allowList = all.allowList.map(_.value)
        )

  final class Verify(historyApi: HistoryApi)(using Executor):

    def apply(all: All, user: User, getTeams: GetUserTeamIds): Fu[WithVerdicts] =
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating)(user, getTeams)

    def rejoin(all: All, user: User, getTeams: GetUserTeamIds): Fu[WithVerdicts] =
      all.withRejoinVerdicts(user, getTeams)

    def canEnter(user: User, getTeams: GetUserTeamIds)(conditions: All): Fu[Boolean] =
      apply(conditions, user, getTeams).dmap(_.accepted)

  import reactivemongo.api.bson.*
  given BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler
