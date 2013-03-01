package lila.user

import lila.common.ConfigSettings
import com.typesafe.config.Config

final class Settings(config: Config) extends ConfigSettings(config getObject "user") {

  val PaginatorMaxPerPage = getInt("paginator.max_per_page")
  val EloUpdaterFloor = getInt("elo_updater.floor")
  val CachedNbTtl = duration("cached.nb.ttl")
  val OnlineTtl = duration("online.ttl")
  val CollectionUser = getString("collection.user")
  val CollectionHistory = getString("collection.history")
  val CollectionConfig = getString("collection.config")
}
