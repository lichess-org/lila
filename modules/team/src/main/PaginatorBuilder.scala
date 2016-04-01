package lila.team

import lila.common.paginator._
import lila.db.dsl._
import lila.db.Implicits._
import lila.db.paginator._
import lila.user.tube.userTube
import lila.user.User
import tube._

private[team] final class PaginatorBuilder(
    maxPerPage: Int,
    maxUserPerPage: Int) {

  def popularTeams(page: Int): Fu[Paginator[Team]] = Paginator(
    adapter = new Adapter[Team](
      selector = TeamRepo.enabledQuery,
      sort = Seq(TeamRepo.sortPopular)),
    page,
    maxPerPage)

  def teamMembers(team: Team, page: Int): Fu[Paginator[MemberWithUser]] = Paginator(
    adapter = new TeamAdapter(team),
    page,
    maxUserPerPage)

  private final class TeamAdapter(team: Team) extends AdapterLike[MemberWithUser] {

    val nbResults = fuccess(team.nbMembers)

    def slice(offset: Int, length: Int): Fu[Seq[MemberWithUser]] = for {
      members ← $find[Member]($query[Member](selector) sort sorting skip offset, length)
      users ← $find.byOrderedIds[User](members.map(_.user))
    } yield members zip users map {
      case (member, user) => MemberWithUser(member, user)
    }
    private def selector = MemberRepo teamQuery team.id
    private def sorting = $sort desc "date"
  }
}
