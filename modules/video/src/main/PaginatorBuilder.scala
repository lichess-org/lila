package lila.video

import lila.common.paginator._
import lila.db.paginator._

private[video] final class PaginatorBuilder(
    videoColl: Coll,
    viewColl: Coll,
    maxPerPage: Int) {

  import handlers._

  def byUser(user: User, page: Int): Fu[Paginator[VideoView]] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: AdapterLike[VideoView], page: Int): Fu[Paginator[VideoView]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User) extends AdapterLike[VideoView] {

    def nbResults: Fu[Int] = $count(selector)

    def slice(offset: Int, length: Int): Fu[Seq[VideoView]] = for {
      gameIds ← $primitive(
        selector,
        "g",
        _ sort sorting skip offset,
        length.some)(_.asOpt[String])
      games ← lila.game.tube.gameTube |> { implicit t =>
        $find.byOrderedIds[Game](gameIds)
      }
    } yield games map { g => VideoView(g, user) }

    private def selector = BookmarkRepo userIdQuery user.id
    private def sorting = $sort desc "d"
  }
}
