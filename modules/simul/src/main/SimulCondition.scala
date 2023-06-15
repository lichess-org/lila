package lila.simul

import lila.rating.PerfType
import lila.user.User
import lila.gathering.{ Condition, ConditionList }
import lila.gathering.Condition.*
import lila.hub.LeaderTeam

object SimulCondition:

  case class All(
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      teamMember: Option[TeamMember]
  ) extends ConditionList(List(maxRating, minRating, teamMember)):

    def withVerdicts(user: User, pt: PerfType, getTeams: GetUserTeamIds, getMaxRating: GetMaxRating)(using
        Executor
    ): Fu[WithVerdicts] =
      list.map {
        case c: MaxRating  => c(getMaxRating)(user, pt) map c.withVerdict
        case c: TeamMember => c(user, getTeams) map c.withVerdict
        case c: FlatCond   => fuccess(c withVerdict c(user, pt))
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
    def apply(simul: Simul, user: User, pt: PerfType)(using
        ex: Executor,
        getTeams: Condition.GetUserTeamIds
    ): Fu[WithVerdicts] =
      val getMaxRating: GetMaxRating = perf => historyApi.lastWeekTopRating(user, perf)
      simul.conditions.withVerdicts(user, pt, getTeams, getMaxRating)
