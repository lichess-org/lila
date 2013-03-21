package lila.message

import lila.db.ReactiveColl

import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  val CollectionThread = config getString "collection.thread"
  val ThreadMaxPerPage = config getInt "thread.max_per_page"

  // lazy val threadRepo = new ThreadRepo(db(CollectionThread))

  def cli = new Cli(this)
}

object Env {

  lazy val current = new Env(
    config = lila.common.PlayApp loadConfig "message",
    db = lila.db.Env.current)
}
