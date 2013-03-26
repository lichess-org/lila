package lila.bookmark

import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }
import lila.common.paginator._
import lila.db.paginator._
import lila.db.Types.Sort

import play.api.libs.json._
import org.joda.time.DateTime

final class PaginatorBuilder(
    bookmarkRepo: BookmarkRepo,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    maxPerPage: Int) extends lila.db.api.Full {

  def byUser(user: User, page: Int): Fu[Paginator[Bookmark]] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: AdapterLike[Bookmark], page: Int): Fu[Paginator[Bookmark]] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )

  final class UserAdapter(user: User) extends AdapterLike[Bookmark] {

    def nbResults: Fu[Int] = count(selector)(bookmarkRepo.coll)

    def slice(offset: Int, length: Int): Fu[Seq[Bookmark]] = {
      val objs = bookmarkRepo find {
    LilaPimpedQueryBuilder(query(selector)).sort(sort: _*) skip offset limit length
      }
        for {
          gameId ← obj.getAs[String]("g")
          date ← obj.getAs[DateTime]("d")
        } yield gameId -> date
      }).flatten
      val games = (gameRepo games objs.map(_._1)).unsafePerformIO
      objs map { obj ⇒
        games find (_.id == obj._1) map { game ⇒
          Bookmark(game, user, obj._2)
        }
      }
    } flatten

    private def selector = bookmarkRepo userIdQuery user.id
    private def sorting = sort desc "d"
  }
}
