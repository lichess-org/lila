package lila.tournament

import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import lila.history.HistoryApi
import lila.hub.LeaderTeam
import lila.rating.PerfType
import alleycats.Zero
import lila.user.{ User, Me }

object TournamentCondition:

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      teamMember: Option[TeamMember],
      allowList: Option[AllowList]
  ) extends ConditionList(List(nbRatedGame, maxRating, minRating, titled, teamMember, allowList)):

    def withVerdicts(perfType: PerfType)(using
        me: Me,
        ex: Executor,
        getMaxRating: GetMaxRating,
        getMyTeamIds: GetMyTeamIds
    ): Fu[WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(perfType) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(perfType))
        case c: TeamMember => c.apply map { c withVerdict _ }
      }.parallel dmap WithVerdicts.apply

    def withRejoinVerdicts(using
        me: Me,
        ex: Executor,
        getMyTeamIds: GetMyTeamIds
    ): Fu[WithVerdicts] =
      list.map {
        case c: TeamMember => c.apply map { c withVerdict _ }
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

    def apply(all: All, perfType: PerfType)(using me: Me)(using GetMyTeamIds): Fu[WithVerdicts] =
      given GetMaxRating = perf => historyApi.lastWeekTopRating(me.value, perf)
      all.withVerdicts(perfType)

    def rejoin(all: All)(using Me)(using GetMyTeamIds): Fu[WithVerdicts] =
      all.withRejoinVerdicts

    def canEnter(perfType: PerfType)(conditions: All)(using Me, GetMyTeamIds): Fu[Boolean] =
      apply(conditions, perfType).dmap(_.accepted)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler
