package lidraughts.team

import lidraughts.common.paginator._
import lidraughts.db.dsl._
import lidraughts.user.{ User, UserRepo }

final class MemberPager(coll: Coll) {

  def apply(team: Team, page: Int, maxPerPage: lidraughts.common.MaxPerPage): Fu[Paginator[User]] =
    Paginator(
      new MemberAdapter(team),
      currentPage = page,
      maxPerPage = maxPerPage
    )

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
