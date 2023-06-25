package lila.simul

import lila.rating.PerfType
import lila.user.Me
import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import lila.hub.LeaderTeam

object SimulCondition:

  case class All(
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      teamMember: Option[TeamMember]
  ) extends ConditionList(List(maxRating, minRating, teamMember)):

    def withVerdicts(pt: PerfType, getMaxRating: GetMaxRating)(using
        Me,
        GetMyTeamIds,
        Executor
    ): Fu[WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(pt) map c.withVerdict
        case c: TeamMember => c.apply map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(pt))
      }.parallel dmap WithVerdicts.apply

  object All:
    val empty = All(none, none, none)

  object form:
    import play.api.data.Forms.*
    import lila.gathering.ConditionForm.*
    def all(leaderTeams: List[LeaderTeam]) =
      mapping(
        "maxRating" -> maxRating,
        "minRating" -> minRating,
        "team"      -> teamMember(leaderTeams)
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler

  final class Verify(historyApi: lila.history.HistoryApi):
    def apply(simul: Simul, pt: PerfType)(using
        me: Me
    )(using Executor, Condition.GetMyTeamIds): Fu[WithVerdicts] =
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(me.value, perf)
      simul.conditions.withVerdicts(pt, getMaxRating)
