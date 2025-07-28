package lila.simul

import lila.core.LightUser.Me
import lila.core.team.LightTeam
import lila.core.user.UserApi
import lila.gathering.Condition.*
import lila.gathering.{ Condition, ConditionList }
import lila.rating.PerfType

object SimulCondition:

  case class All(
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      teamMember: Option[TeamMember],
      accountAge: Option[AccountAge]
  ) extends ConditionList(List(maxRating, minRating, teamMember, accountAge)):

    def withVerdicts(
        pt: PerfType
    )(using Me, Perf, GetMyTeamIds, GetMaxRating, GetAge, Executor): Fu[WithVerdicts] =
      list
        .parallel:
          case c: MaxRating => c(pt).map(c.withVerdict)
          case c: TeamMember => c.apply.map(c.withVerdict)
          case c: AccountAge => c.apply.map(c.withVerdict(_))
          case c: FlatCond => fuccess(c.withVerdict(c(pt)))
        .dmap(WithVerdicts.apply)

  object All:
    val empty = All(None, None, None, None)

  object form:
    import play.api.data.Forms.*
    import lila.gathering.ConditionForm.*
    def all(leaderTeams: List[LightTeam]) =
      mapping(
        "maxRating" -> maxRating,
        "minRating" -> minRating,
        "team" -> teamMember(leaderTeams),
        "accountAge" -> accountAge
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler

  final class Verify(historyApi: lila.core.history.HistoryApi, userApi: UserApi):
    def apply(simul: Simul, pt: PerfType)(using
        me: Me
    )(using Executor, Condition.GetMyTeamIds, Perf): Fu[WithVerdicts] =
      given GetMaxRating = historyApi.lastWeekTopRating(me.userId, _)
      given GetAge = me => userApi.accountAge(me.userId)
      simul.conditions.withVerdicts(pt)
