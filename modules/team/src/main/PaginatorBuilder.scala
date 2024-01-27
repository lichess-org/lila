package lila.team

import lila.common.config.MaxPerPage
import lila.common.paginator.*
import lila.common.LightUser
import lila.db.dsl.{ *, given }
import lila.db.paginator.*
import lila.user.MyId

final private[team] class PaginatorBuilder(
    teamRepo: TeamRepo,
    memberRepo: TeamMemberRepo,
    requestRepo: TeamRequestRepo,
    userApi: lila.user.UserApi,
    lightUserApi: lila.user.LightUserApi
)(using Executor):
  private val maxPerPage         = MaxPerPage(15)
  private val maxUserPerPage     = MaxPerPage(30)
  private val maxRequestsPerPage = MaxPerPage(10)

  import BSONHandlers.given

  def popularTeams(page: Int)(using me: Option[MyId]): Fu[Paginator[Team.WithMyLeadership]] =
    Paginator(
      adapter = popularTeamsAdapter(page).mapFutureList(memberRepo.addMyLeadership),
      page,
      maxPerPage
    )

  def popularTeamsWithPublicLeaders(page: Int): Fu[Paginator[Team.WithPublicLeaderIds]] =
    Paginator(
      adapter = popularTeamsAdapter(page).mapFutureList(memberRepo.addPublicLeaderIds),
      page,
      maxPerPage
    )

  private def popularTeamsAdapter(page: Int): Adapter[Team] =
    Adapter[Team](
      collection = teamRepo.coll,
      selector = teamRepo.enabledSelect,
      projection = none,
      sort = teamRepo.sortPopular
    )

  def teamMembers(team: Team, page: Int): Fu[Paginator[LightUser]] =
    Paginator(
      adapter = TeamAdapter(team),
      page,
      maxUserPerPage
    )

  def teamMembersWithDate(team: Team, page: Int): Fu[Paginator[TeamMember.UserAndDate]] =
    Paginator(
      adapter = TeamAdapterWithDate(team),
      page,
      maxUserPerPage
    )

  private trait MembersAdapter:
    val team: Team
    val nbResults = fuccess(team.nbMembers)
    val sorting   = $sort desc "date"
    val selector  = memberRepo teamQuery team.id

  final private class TeamAdapter(val team: Team) extends AdapterLike[LightUser] with MembersAdapter:
    def slice(offset: Int, length: Int): Fu[Seq[LightUser]] =
      for
        docs <-
          memberRepo.coll
            .find(selector, $doc("user" -> true, "_id" -> false).some)
            .sort(sorting)
            .skip(offset)
            .cursor[Bdoc]()
            .list(length)
        userIds = docs.flatMap(_.getAsOpt[UserId]("user"))
        users <- lightUserApi asyncManyFallback userIds
      yield users

  final private class TeamAdapterWithDate(val team: Team)
      extends AdapterLike[TeamMember.UserAndDate]
      with MembersAdapter:
    def slice(offset: Int, length: Int): Fu[Seq[TeamMember.UserAndDate]] =
      for
        docs <-
          memberRepo.coll
            .find(selector, $doc("user" -> true, "date" -> true, "_id" -> false).some)
            .sort(sorting)
            .skip(offset)
            .cursor[Bdoc]()
            .list(length)
        userIds = docs.flatMap(_.getAsOpt[UserId]("user"))
        dates   = docs.flatMap(_.getAsOpt[Instant]("date"))
        users <- lightUserApi asyncManyFallback userIds
      yield users.zip(dates) map TeamMember.UserAndDate.apply

  def declinedRequests(team: Team, page: Int): Fu[Paginator[RequestWithUser]] =
    Paginator(
      adapter = DeclinedRequestAdapter(team),
      page,
      maxRequestsPerPage
    )
  final private class DeclinedRequestAdapter(team: Team) extends AdapterLike[RequestWithUser]:
    val nbResults        = requestRepo countDeclinedByTeam team.id
    private def selector = requestRepo teamDeclinedQuery team.id
    private def sorting  = $sort desc "date"

    def slice(offset: Int, length: Int): Fu[Seq[RequestWithUser]] =
      for
        requests <- requestRepo.coll
          .find(selector)
          .sort(sorting)
          .skip(offset)
          .cursor[TeamRequest]()
          .list(length)
        users <- userApi.listWithPerfs(requests.map(_.user))
      yield RequestWithUser.combine(requests, users)
