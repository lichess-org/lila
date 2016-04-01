package lila.bookmark

import lila.db.dsl._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo }
import lila.user.User
import tube.bookmarkTube

final class BookmarkApi(
    cached: Cached,
    paginator: PaginatorBuilder) {

  def toggle(gameId: String, userId: String): Funit =
    $find.byId[Game](gameId) flatMap {
      _ ?? { game =>
        BookmarkRepo.toggle(gameId, userId) flatMap { bookmarked =>
          GameRepo.incBookmarks(gameId, bookmarked.fold(1, -1)) >>-
            (cached.gameIdsCache invalidate userId)
        }
      }
    }

  def bookmarked(game: Game, user: User): Boolean = cached.bookmarked(game.id, user.id)

  def gameIds(userId: String): Set[String] = cached gameIds userId

  def countByUser(user: User): Int = cached count user.id

  def removeByGameId(id: String): Funit = BookmarkRepo removeByGameId id

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user, page) map2 { (b: Bookmark) => b.game }
}
