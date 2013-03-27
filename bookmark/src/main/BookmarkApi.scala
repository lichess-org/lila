package lila.bookmark

import lila.game.{ Game, GameRepo }
import lila.user.{ User, UserRepo }

import play.api.libs.concurrent.Execution.Implicits._

final class BookmarkApi(
    bookmarkRepo: BookmarkRepo,
    cached: Cached,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    paginator: PaginatorBuilder) {

  def toggle(gameId: String, userId: String): Funit =
    gameRepo.find byId gameId flatMap { gameOption ⇒
      gameOption zmap { game ⇒
        bookmarkRepo.toggle(gameId, userId) flatMap { bookmarked ⇒
          gameRepo.incBookmarks(gameId, bookmarked.fold(1, -1)) >>
            fuccess(cached invalidateUserId userId)
        }
      }
    }

  def bookmarked(game: Game, user: User): Fu[Boolean] = cached.bookmarked(game.id, user.id)

  def countByUser(user: User): Fu[Int] = cached.count(user.id)

  def userIdsByGame(game: Game): Fu[List[String]] =
    game.hasBookmarks ?? (bookmarkRepo userIdsByGameId game.id)

  def removeByGame(game: Game): Funit = bookmarkRepo removeByGameId game.id

  def removeByGameIds(ids: List[String]): Funit = bookmarkRepo removeByGameIds ids

  def gamePaginatorByUser(user: User, page: Int) =
    paginator.byUser(user, page) map2 { (b: Bookmark) ⇒ b.game }
}
