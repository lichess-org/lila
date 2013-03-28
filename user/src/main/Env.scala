package lila.user

import lila.db.Types.Coll
import lila.common.PimpedConfig._

import chess.EloCalculator
import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  val PaginatorMaxPerPage = config getInt "paginator.max_per_page"
  val EloUpdaterFloor = config getInt "elo_updater.floor"
  val CachedNbTtl = config duration "cached.nb.ttl"
  val OnlineTtl = config duration "online.ttl"
  val CollectionUser = config getString "collection.user"
  val CollectionHistory = config getString "collection.history"
  val CollectionConfig = config getString "collection.config"

  lazy val historyColl = db(CollectionHistory)

  lazy val userColl = db(CollectionUser)

  lazy val paginator = new PaginatorBuilder(
    countUsers = cached.countEnabled,
    maxPerPage = PaginatorMaxPerPage)

  lazy val eloUpdater = new EloUpdater(floor = EloUpdaterFloor)

  lazy val usernameMemo = new UsernameMemo(ttl = OnlineTtl)

  lazy val cached = new Cached(ttl = CachedNbTtl)

  def usernameOrAnonymous(id: String): Fu[String] = 
    cached usernameOrAnonymous id

  def cli = new Cli(this)
}

object Env {

  lazy val current: Env = "[user] boot" describes new Env(
    config = lila.common.PlayApp loadConfig "user",
    db = lila.db.Env.current) 
}
