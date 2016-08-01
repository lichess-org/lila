package lila.bookmark

import lila.db.dsl._
import lila.game.{ Game, GameRepo }
import lila.user.User

final class BookmarkApi(
    coll: Coll,
    cached: Cached,
    paginator: PaginatorBuilder) {

  import lila.game.BSONHandlers.gameBSONHandler

  def toggle(gameId: String, userId: String): Funit =
    GameRepo game gameId flatMap {
      _ ?? { game =>
        BookmarkRepo.toggle(gameId, userId) flatMap { bookmarked =>
          GameRepo.incBookmarks(gameId, bookmarked.fold(1, -1)) >>-
            (cached invalidate userId)
        }
      }
    }

  def bookmarked(game: Game, user: User): Boolean = cached.bookmarked(game.id, user.id)

  def gameIds(userId: String): Fu[Set[String]] = cached gameIds userId

  def countByUser(user: User): Fu[Int] = cached count user.id

  def removeByGameId(id: String): Funit = BookmarkRepo removeByGameId id

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user, page) map2 { (b: Bookmark) => b.game }
}
