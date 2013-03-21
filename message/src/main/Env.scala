package lila.message

import lila.db.ReactiveColl
import lila.user.UserRepo

import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env, userRepo: UserRepo) {

  val CollectionThread = config getString "collection.thread"
  val ThreadMaxPerPage = config getInt "thread.max_per_page"

  lazy val threadRepo = new ThreadRepo(db(CollectionThread))

  lazy val unreadCache = new UnreadCache(threadRepo)

  lazy val forms = new DataForm(userRepo)

  def cli = new Cli(this)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current,
    userRepo = lila.user.Env.current.userRepo)
}
