package lila.team

import lila.common.paginator._
import lila.common.MaxPerPage
import lila.db.dsl._
import lila.db.paginator._
import lila.user.UserRepo

private[team] final class PaginatorBuilder(
    coll: Colls,
    maxPerPage: MaxPerPage,
    maxUserPerPage: MaxPerPage
) {

  import BSONHandlers._

  def popularTeams(page: Int): Fu[Paginator[Team]] = Paginator(
    adapter = new Adapter(
      collection = coll.team,
      selector = TeamRepo.enabledQuery,
      projection = $empty,
      sort = TeamRepo.sortPopular
    ),
    page,
    maxPerPage
  )

  def teamMembers(team: Team, page: Int): Fu[Paginator[MemberWithUser]] = Paginator(
    adapter = new TeamAdapter(team),
    page,
    maxUserPerPage
  )

  private final class TeamAdapter(team: Team) extends AdapterLike[MemberWithUser] {

    val nbResults = fuccess(team.nbMembers)

    def slice(offset: Int, length: Int): Fu[Seq[MemberWithUser]] = for {
      members ← coll.member.find(selector)
        .sort(sorting).skip(offset).cursor[Member]().gather[List](length)
      users ← UserRepo usersFromSecondary members.map(_.user)
    } yield members zip users map {
      case (member, user) => MemberWithUser(member, user)
    }
    private def selector = MemberRepo teamQuery team.id
    private def sorting = $sort desc "date"
  }
}
