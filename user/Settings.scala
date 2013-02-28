package lila.user

import lila.common.ConfigSettings
import com.typesafe.config.Config

final class Settings(config: Config) extends ConfigSettings(config getObject "user") {

  val UserPaginatorMaxPerPage = getInt("paginator.max_per_page")
  val UserEloUpdaterFloor = getInt("elo_updater.floor")
  val UserCachedNbTtl = millis("cached.nb.ttl")
  val UserCollectionUser = getString("collection.user")
  val UserCollectionHistory = getString("collection.history")
  val UserCollectionConfig = getString("collection.config")
}
