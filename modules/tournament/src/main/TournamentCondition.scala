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
        getMaxRating: GetMaxRating,
        getUserTeamIds: User => Fu[List[TeamId]]
    )(user: User, perfType: PerfType)(using Executor): Fu[WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user, perfType) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user, perfType))
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

    def validRatings = (minRating, maxRating) match
      case (Some(min), Some(max)) => min.rating < max.rating
      case _                      => true

  object All:
    val empty             = All(none, none, none, none, none, none)
    given zero: Zero[All] = Zero(empty)

  object form:
    import play.api.data.Forms.*
    import lila.common.Form.{ *, given }
    import lila.gathering.ConditionForm.*
    def all(leaderTeams: List[LeaderTeam]) =
      mapping(
        "nbRatedGame" -> optional(nbRatedGame),
        "maxRating"   -> optional(maxRating),
        "minRating"   -> optional(minRating),
        "titled"      -> titled,
        "teamMember"  -> optional(teamMember(leaderTeams)),
        "allowList"   -> optional(allowList)
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  final class Verify(historyApi: HistoryApi)(using Executor):

    def apply(all: All, user: User, perfType: PerfType, getTeams: GetUserTeamIds): Fu[WithVerdicts] =
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(getMaxRating, getTeams)(user, perfType)

    def rejoin(all: All, user: User, getTeams: GetUserTeamIds): Fu[WithVerdicts] =
      all.withRejoinVerdicts(user, getTeams)

    def canEnter(user: User, perfType: PerfType, getTeams: GetUserTeamIds)(conditions: All): Fu[Boolean] =
      apply(conditions, user, perfType, getTeams).dmap(_.accepted)

  import reactivemongo.api.bson.*
  given BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler
