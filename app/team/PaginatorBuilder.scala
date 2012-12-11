package lila
package team

import user.{ User, UserRepo }
import mongodb.CachedAdapter

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

final class PaginatorBuilder(
    memberRepo: MemberRepo,
    teamRepo: TeamRepo,
    userRepo: UserRepo,
    maxPerPage: Int) {

  def popularTeams(page: Int): Paginator[Team] = paginator(
    SalatAdapter(
      dao = teamRepo,
      query = teamRepo.enabledQuery,
      sort = teamRepo.sortPopular), page)

  def teamMembers(team: Team, page: Int): Paginator[MemberWithUser] =
    paginator(new TeamAdapter(team), page)

  private def paginator[A](adapter: Adapter[A], page: Int): Paginator[A] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    ) | paginator(adapter, 1)

  final class TeamAdapter(team: Team) extends Adapter[MemberWithUser] {

    val nbResults = team.nbMembers

    def slice(offset: Int, length: Int): Seq[MemberWithUser] = {
      val members = (memberRepo find query sort sort skip offset limit length).toList
      val users = (userRepo byOrderedIds members.map(_.user)).unsafePerformIO
      members zip users map {
        case (member, user) ⇒ MemberWithUser(member, user)
      }
    }

    private def query = memberRepo teamIdQuery team.id
    private def sort = memberRepo sortQuery -1
  }
}
