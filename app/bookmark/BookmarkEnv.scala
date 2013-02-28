package lila.app
package bookmark

import com.mongodb.casbah.MongoCollection

import game.GameRepo
import user.UserRepo
import core.Settings

final class BookmarkEnv(
    settings: Settings,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    mongodb: String ⇒ MongoCollection) {

  import settings._

  lazy val bookmarkRepo = new BookmarkRepo(mongodb(BookmarkCollectionBookmark))

  lazy val paginator = new PaginatorBuilder(
    bookmarkRepo = bookmarkRepo,
    gameRepo = gameRepo,
    userRepo = userRepo,
    maxPerPage = GamePaginatorMaxPerPage)

  lazy val cached = new Cached(
    bookmarkRepo = bookmarkRepo)

  lazy val api = new BookmarkApi(
    bookmarkRepo = bookmarkRepo,
    cached = cached,
    gameRepo = gameRepo,
    userRepo = userRepo,
    paginator = paginator)
}
