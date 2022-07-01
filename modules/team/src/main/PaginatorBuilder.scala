package lila.team

import lila.common.config.MaxPerPage
import lila.common.paginator._
import lila.common.LightUser
import lila.db.dsl._
import lila.db.paginator._
import org.joda.time.DateTime

final private[team] class PaginatorBuilder(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    userRepo: lila.user.UserRepo,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {
  private val maxPerPage         = MaxPerPage(15)
  private val maxUserPerPage     = MaxPerPage(30)
  private val maxRequestsPerPage = MaxPerPage(10)

  import BSONHandlers._

  def popularTeams(page: Int): Fu[Paginator[Team]] =
    Paginator(
      adapter = new Adapter(
        collection = teamRepo.coll,
        selector = teamRepo.enabledSelect,
        projection = none,
        sort = teamRepo.sortPopular
      ),
      page,
      maxPerPage
    )

  def teamMembers(team: Team, page: Int): Fu[Paginator[LightUser]] =
    Paginator(
      adapter = new TeamAdapter(team),
      page,
      maxUserPerPage
    )

  def teamMembersWithDate(team: Team, page: Int): Fu[Paginator[TeamMember.UserAndDate]] =
    Paginator(
      adapter = new TeamAdapterWithDate(team),
      page,
      maxUserPerPage
    )

  private trait MembersAdapter {
    val team: Team
    val nbResults = fuccess(team.nbMembers)
    val sorting   = $sort desc "date"
    val selector  = memberRepo teamQuery team.id
  }

  final private class TeamAdapter(val team: Team) extends AdapterLike[LightUser] with MembersAdapter {
    def slice(offset: Int, length: Int): Fu[Seq[LightUser]] =
      for {
        docs <-
          memberRepo.coll
            .find(selector, $doc("user" -> true, "_id" -> false).some)
            .sort(sorting)
            .skip(offset)
            .cursor[Bdoc]()
            .list(length)
        userIds = docs.flatMap(_ string "user")
        users <- lightUserApi asyncMany userIds
      } yield users.flatten
  }

  final private class TeamAdapterWithDate(val team: Team)
      extends AdapterLike[TeamMember.UserAndDate]
      with MembersAdapter {
    def slice(offset: Int, length: Int): Fu[Seq[TeamMember.UserAndDate]] =
      for {
        docs <-
          memberRepo.coll
            .find(selector, $doc("user" -> true, "date" -> true, "_id" -> false).some)
            .sort(sorting)
            .skip(offset)
            .cursor[Bdoc]()
            .list(length)
        userIds = docs.flatMap(_ string "user")
        dates   = docs.flatMap(_.getAsOpt[DateTime]("date"))
        users <- lightUserApi asyncMany userIds
      } yield users.flatten.zip(dates) map { case (u, d) => TeamMember.UserAndDate(u, d) }
  }

  def declinedRequests(team: Team, page: Int): Fu[Paginator[RequestWithUser]] =
    Paginator(
      adapter = new DeclinedRequestAdapter(team),
      page,
      maxRequestsPerPage
    )
  final private class DeclinedRequestAdapter(team: Team) extends AdapterLike[RequestWithUser] {
    val nbResults        = requestRepo countDeclinedByTeam team.id
    private def selector = requestRepo teamDeclinedQuery team.id
    private def sorting  = $sort desc "date"

    def slice(offset: Int, length: Int): Fu[Seq[RequestWithUser]] = {
      for {
        requests <- requestRepo.coll
          .find(selector)
          .sort(sorting)
          .skip(offset)
          .cursor[Request]()
          .list(length)
        users <- userRepo usersFromSecondary requests.map(_.user)
      } yield requests zip users map { case (request, user) => RequestWithUser(request, user) }
    }
  }

}
