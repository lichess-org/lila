package lila
package bookmark

import game.{ DbGame, GameRepo }
import user.{ User, UserRepo }

import scalaz.effects._

final class BookmarkApi(
    bookmarkRepo: BookmarkRepo,
    cached: Cached,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    paginator: PaginatorBuilder) {

  def toggle(gameId: String, userId: String): IO[Unit] = for {
    gameOption ← gameRepo game gameId
    _ ← ~gameOption.map(game =>
      for {
        bookmarked ← bookmarkRepo.toggle(gameId, userId)
        _ ← gameRepo.incBookmarks(gameId, bookmarked.fold(1, -1))
        _ ← io(cached invalidateUserId userId)
      } yield ()
    )
  } yield ()

  def bookmarked(game: DbGame, user: User): Boolean =
    cached.bookmarked(game.id, user.id)

  def countByUser(user: User): Int =
    cached.count(user.id)

  def userIdsByGame(game: DbGame): IO[List[String]] = 
    if (game.hasBookmarks) bookmarkRepo userIdsByGameId game.id
    else io(Nil)

  def removeByGame(game: DbGame): IO[Unit] =
    bookmarkRepo removeByGameId game.id

  def removeByGameIds(ids: List[String]): IO[Unit] =
    bookmarkRepo removeByGameIds ids

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user: User, page: Int) map (_.game)
}
