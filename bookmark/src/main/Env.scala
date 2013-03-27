package lila.bookmark

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env,
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo) {

  private val CollectionBookmark = config getString "collection.bookmark"
  private val PaginatorMaxPerPage = config getInt "paginator.max_per_page"

  private lazy val bookmarkRepo = new BookmarkRepo()(db(CollectionBookmark))

  private lazy val cached = new Cached(bookmarkRepo)

  lazy val paginator = new PaginatorBuilder(
    bookmarkRepo = bookmarkRepo,
    gameRepo = gameRepo,
    userRepo = userRepo,
    maxPerPage = PaginatorMaxPerPage)

  lazy val api = new BookmarkApi(
    bookmarkRepo = bookmarkRepo,
    cached = cached,
    gameRepo = gameRepo,
    userRepo = userRepo,
    paginator = paginator)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    db = lila.db.Env.current,
    userRepo = lila.user.Env.current.userRepo,
    gameRepo = lila.game.Env.current.gameRepo)
}
