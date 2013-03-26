package lila.bookmark

import com.typesafe.config.Config
import lila.common.PimpedConfig._
import akka.actor._

final class Env(
    config: Config,
    db: lila.db.Env,
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo) {

  val CollectionBookmark = config getString "collection.bookmark"

  lazy val bookmarkRepo = new BookmarkRepo()(db(CollectionBookmark))
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "bookmark",
    db = lila.db.Env.current,
    userRepo = lila.user.Env.current.userRepo,
    gameRepo = lila.game.Env.current.gameRepo)
}
