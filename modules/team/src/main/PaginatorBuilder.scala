package lila.team

import lila.common.config.MaxPerPage
import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._

private[team] final class PaginatorBuilder(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    userRepo: lila.user.UserRepo
) {
  private val maxPerPage = MaxPerPage(15)
  private val maxUserPerPage = MaxPerPage(24)

  import BSONHandlers._

  def popularTeams(page: Int): Fu[Paginator[Team]] = Paginator(
    adapter = new Adapter(
      collection = teamRepo.coll,
      selector = teamRepo.enabledQuery,
      projection = none,
      sort = teamRepo.sortPopular
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
      members <- memberRepo.coll.ext.find(selector)
        .sort(sorting).skip(offset).cursor[Member]().gather[List](length)
      users <- userRepo usersFromSecondary members.map(_.user)
    } yield members zip users map {
      case (member, user) => MemberWithUser(member, user)
    }
    private def selector = memberRepo teamQuery team.id
    private def sorting = $sort desc "date"
  }
}
