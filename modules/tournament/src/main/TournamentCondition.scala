package lila.tournament

import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import lila.user.User
import lila.history.HistoryApi
import lila.hub.LeaderTeam
import lila.rating.PerfType
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

    def withVerdicts(user: User, perfType: PerfType)(using
        ex: Executor,
        getMaxRating: GetMaxRating,
        getUserTeamIds: GetUserTeamIds
    ): Fu[WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user, perfType) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user, perfType))
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
      }.parallel dmap WithVerdicts.apply

    def withRejoinVerdicts(user: User)(using
        ex: Executor,
        getUserTeamIds: GetUserTeamIds
    ): Fu[WithVerdicts] =
      list.map {
        case c: TeamMember => c(user, getUserTeamIds) map { c withVerdict _ }
        case c             => fuccess(WithVerdict(c, Accepted))
      }.parallel dmap WithVerdicts.apply

    def similar(other: All) = sameRatings(other) && titled == other.titled && teamMember == other.teamMember

  object All:
    val empty             = All(none, none, none, none, none, none)
    given zero: Zero[All] = Zero(empty)

  object form:
    import play.api.data.Forms.*
    import lila.gathering.ConditionForm.*
    def all(leaderTeams: List[LeaderTeam]) =
      mapping(
        "nbRatedGame" -> nbRatedGame,
        "maxRating"   -> maxRating,
        "minRating"   -> minRating,
        "titled"      -> titled,
        "teamMember"  -> teamMember(leaderTeams),
        "allowList"   -> allowList
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  final class Verify(historyApi: HistoryApi)(using Executor):

    def apply(all: All, user: User, perfType: PerfType)(using getTeams: GetUserTeamIds): Fu[WithVerdicts] =
      given GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      all.withVerdicts(user, perfType)

    def rejoin(all: All, user: User)(using getTeams: GetUserTeamIds): Fu[WithVerdicts] =
      all.withRejoinVerdicts(user)

    def canEnter(user: User, perfType: PerfType)(conditions: All)(using
        getTeams: GetUserTeamIds
    ): Fu[Boolean] =
      apply(conditions, user, perfType).dmap(_.accepted)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler
