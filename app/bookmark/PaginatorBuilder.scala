package lila
package bookmark

import game.{ DbGame, GameRepo }
import user.{ User, UserRepo }

import com.github.ornicar.paginator._
import com.mongodb.casbah.Imports._
import org.joda.time.DateTime

final class PaginatorBuilder(
    bookmarkRepo: BookmarkRepo,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    maxPerPage: Int) {

  def byUser(user: User, page: Int): Paginator[Bookmark] =
    paginator(new UserAdapter(user), page)

  private def paginator(adapter: Adapter[Bookmark], page: Int): Paginator[Bookmark] =
    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    ).fold(_ ⇒ paginator(adapter, 0), identity)

  final class UserAdapter(user: User) extends Adapter[Bookmark] {

    def nbResults: Int = bookmarkRepo.collection count query toInt

    def slice(offset: Int, length: Int): Seq[Bookmark] = {
      val objs = ((bookmarkRepo.collection find query sort sort skip offset limit length).toList map { obj ⇒
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

    private def query = bookmarkRepo userIdQuery user.id
    private def sort = bookmarkRepo sortQuery -1
  }
}
