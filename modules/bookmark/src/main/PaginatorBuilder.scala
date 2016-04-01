package lila.bookmark

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._

import lila.common.paginator._
import lila.common.PimpedJson._
import lila.db.dsl._
import lila.db.Implicits._
import lila.db.paginator._
import lila.game.{ Game, GameRepo }
import lila.user.User
import tube.bookmarkTube

private[bookmark] final class PaginatorBuilder(maxPerPage: Int) {

  def byUser(user: User, page: Int): Fu[Paginator[Bookmark]] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: AdapterLike[Bookmark], page: Int): Fu[Paginator[Bookmark]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User) extends AdapterLike[Bookmark] {

    def nbResults: Fu[Int] = $count(selector)

    def slice(offset: Int, length: Int): Fu[Seq[Bookmark]] = for {
      gameIds ← $primitive(
        selector,
        "g",
        _ sort sorting skip offset,
        length.some)(_.asOpt[String])
      games ← lila.game.tube.gameTube |> { implicit t =>
        $find.byOrderedIds[Game](gameIds)
      }
    } yield games map { g => Bookmark(g, user) }

    private def selector = BookmarkRepo userIdQuery user.id
    private def sorting = $sort desc "d"
  }
}
