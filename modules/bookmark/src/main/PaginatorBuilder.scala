package lidraughts.bookmark

import lidraughts.common.paginator._
import lidraughts.db.dsl._
import lidraughts.game.GameRepo
import lidraughts.user.User

private[bookmark] final class PaginatorBuilder(
    coll: Coll,
    maxPerPage: lidraughts.common.MaxPerPage
) {

  def byUser(user: User, page: Int): Fu[Paginator[Bookmark]] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: AdapterLike[Bookmark], page: Int): Fu[Paginator[Bookmark]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User) extends AdapterLike[Bookmark] {

    def nbResults: Fu[Int] = coll countSel selector

    def slice(offset: Int, length: Int): Fu[Seq[Bookmark]] = for {
      gameIds ← coll.find(selector, $doc("g" -> true))
        .sort(sorting)
        .skip(offset)
        .cursor[Bdoc]()
        .gather[List](length) map { _ flatMap { _.getAs[String]("g") } }
      games ← GameRepo gamesFromSecondary gameIds
    } yield games map { g => Bookmark(g, user) }

    private def selector = $doc("u" -> user.id)
    private def sorting = $sort desc "d"
  }
}
