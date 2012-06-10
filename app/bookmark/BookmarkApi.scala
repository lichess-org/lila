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

  def toggle(gameId: String, user: User): IO[Unit] = for {
    gameOption ← gameRepo game gameId
    _ ← gameOption.fold(
      game ⇒ for {
        bookmarked ← bookmarkRepo.toggle(game.id, user.id)
        _ ← gameRepo.incBookmarks(game.id, bookmarked.fold(1, -1))
        _ ← io(cached invalidateUserId user.id)
      } yield (),
      io())
  } yield ()

  def bookmarked(game: DbGame, user: User): Boolean =
    cached.bookmarked(game.id, user.id)

  def countByUser(user: User): Int =
    cached.count(user.id)

  def usersByGame(game: DbGame): IO[List[User]] = 
    if (game.hasBookmarks) for {
      userIds ← bookmarkRepo userIdsByGameId game.id
      users ← (userIds map userRepo.byId).sequence
    } yield users.flatten
    else io(Nil)

  def removeByGame(game: DbGame): IO[Unit] =
    bookmarkRepo removeByGameId game.id

  def removeByGameIds(ids: List[String]): IO[Unit] =
    bookmarkRepo removeByGameIds ids

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user: User, page: Int) map (_.game)
}
