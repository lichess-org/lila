package lila.team

import lila.common.paginator._
import lila.db.dsl._
import lila.db.paginator._
import lila.user.{ User, UserRepo }

final class MemberPager(coll: Coll) {

  def apply(team: Team, page: Int, maxPerPage: Int): Fu[Paginator[User]] =
    Paginator(
      new MemberAdapter(team),
      currentPage = page,
      maxPerPage = maxPerPage)

  final class MemberAdapter(team: Team) extends AdapterLike[User] {

    def nbResults: Fu[Int] = fuccess(team.nbMembers)

    def slice(offset: Int, length: Int): Fu[Seq[User]] =
      coll.find($doc("team" -> team.id), $doc("user" -> true))
        .sort($sort desc "date")
        .skip(offset)
        .cursor[Bdoc]()
        .gather[List](length) map {
          _ flatMap { _.getAs[String]("user") }
        } flatMap UserRepo.usersFromSecondary
  }
}
