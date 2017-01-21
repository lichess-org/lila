package lila.memo

import com.typesafe.config.Config

final class Env(config: Config, db: lila.db.Env) {

  private val CollectionCache = config getString "collection.cache"
  private val CollectionConfig = config getString "collection.config"

  lazy val mongoCache: MongoCache.Builder = MongoCache(db(CollectionCache))

  lazy val configStore: ConfigStore.Builder = ConfigStore(db(CollectionConfig))
}

object Env {

  lazy val current = "memo" boot new Env(
    lila.common.PlayApp loadConfig "memo",
    lila.db.Env.current)
}
