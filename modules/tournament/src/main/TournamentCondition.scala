package lila.tournament

import alleycats.Zero

import lila.core.history.HistoryApi
import lila.core.team.LightTeam
import lila.core.user.UserApi
import lila.gathering.Condition.*
import lila.gathering.{ Condition, ConditionList }
import lila.rating.PerfType

object TournamentCondition:

  case class All(
      nbRatedGame: Option[NbRatedGame],
      maxRating: Option[MaxRating],
      minRating: Option[MinRating],
      titled: Option[Titled.type],
      teamMember: Option[TeamMember],
      accountAge: Option[AccountAge],
      allowList: Option[AllowList],
      bots: Option[Bots]
  ) extends ConditionList(
        List(nbRatedGame, maxRating, minRating, titled, teamMember, accountAge, allowList, bots)
      ):

    private def listWithBots = if bots.isDefined then list else Bots(false) :: list

    def withVerdicts(perfType: PerfType)(using
        Me,
        Perf,
        Executor,
        GetMaxRating,
        GetMyTeamIds,
        GetAge
    ): Fu[WithVerdicts] =
      listWithBots
        .parallel:
          case c: MaxRating => c(perfType).map(c.withVerdict)
          case c: FlatCond => fuccess(c.withVerdict(c(perfType)))
          case c: TeamMember => c.apply.map { c.withVerdict(_) }
          case c: AccountAge => c.apply.map { c.withVerdict(_) }
        .dmap(WithVerdicts.apply)

    def withRejoinVerdicts(using
        me: Me,
        ex: Executor,
        getMyTeamIds: GetMyTeamIds
    ): Fu[WithVerdicts] =
      listWithBots
        .parallel:
          case c: TeamMember => c.apply.map { c.withVerdict(_) }
          case c => fuccess(WithVerdict(c, Accepted))
        .dmap(WithVerdicts.apply)

    def similar(other: All) = sameRatings(other) && titled == other.titled && teamMember == other.teamMember

    // if the new allowList is empty, assume the tournament is open to all, kick nobody
    def removedFromAllowList(prev: All): Set[UserId] =
      allowList
        .so(_.userIds)
        .nonEmptyOption
        .so: current =>
          prev.allowList.so(_.userIds.diff(current))

    def allowsBots = bots.exists(_.allowed)

  object All:
    val empty = All(none, none, none, none, none, none, none, none)
    given zero: Zero[All] = Zero(empty)

  object form:
    import play.api.data.Forms.*
    import lila.gathering.ConditionForm.*
    def all(leaderTeams: List[LightTeam]) =
      mapping(
        "nbRatedGame" -> nbRatedGame,
        "maxRating" -> maxRating,
        "minRating" -> minRating,
        "titled" -> titled,
        "teamMember" -> teamMember(leaderTeams),
        "accountAge" -> accountAge,
        "allowList" -> allowList,
        "bots" -> bots
      )(All.apply)(unapply).verifying("Invalid ratings", _.validRatings)

  final class Verify(historyApi: HistoryApi, userApi: UserApi)(using Executor):

    def apply(all: All, perfType: PerfType)(using me: Me)(using GetMyTeamIds, Perf): Fu[WithVerdicts] =
      given GetMaxRating = historyApi.lastWeekTopRating(me.userId, _)
      given GetAge = me => userApi.accountAge(me.userId)
      all.withVerdicts(perfType)

    def rejoin(all: All)(using Me)(using GetMyTeamIds): Fu[WithVerdicts] =
      all.withRejoinVerdicts

    def canEnter(perfType: PerfType)(conditions: All)(using Me, GetMyTeamIds, Perf): Fu[Boolean] =
      apply(conditions, perfType).dmap(_.accepted)

  import reactivemongo.api.bson.*
  given bsonHandler: BSONDocumentHandler[All] =
    import lila.gathering.ConditionHandlers.BSONHandlers.given
    Macros.handler
