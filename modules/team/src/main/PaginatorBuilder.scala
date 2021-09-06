package lila.team

import com.softwaremill.tagging._

import lila.common.config.MaxPerPage
import lila.common.paginator._
import lila.common.LightUser
import lila.db.dsl._
import lila.db.paginator._

final private[team] class PaginatorBuilder(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    declinedRequestRepo: RequestRepo @@ DeclinedRequest,
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

  final private class TeamAdapter(team: Team) extends AdapterLike[LightUser] {

    val nbResults = fuccess(team.nbMembers)

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
    private def selector = memberRepo teamQuery team.id
    private def sorting  = $sort desc "date"
  }

  def declinedRequests(team: Team, page: Int): Fu[Paginator[RequestWithUser]] =
    Paginator(
      adapter = new DeclinedRequestAdapter(team),
      page,
      maxRequestsPerPage
    )
  final private class DeclinedRequestAdapter(team: Team) extends AdapterLike[RequestWithUser] {
    val nbResults        = declinedRequestRepo countByTeam team.id
    private def selector = declinedRequestRepo teamQuery team.id
    private def sorting  = $sort desc "date"

    def slice(offset: Int, length: Int): Fu[Seq[RequestWithUser]] = {
      for {
        requests <- declinedRequestRepo.coll
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
