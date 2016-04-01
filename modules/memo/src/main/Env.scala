package lila.memo

import com.typesafe.config.Config
import lila.db.dsl._

final class Env(config: Config, db: lila.db.Env) {

  private val CollectionCache = config getString "collection.cache"

  lazy val mongoCache: MongoCache.Builder = MongoCache(db(CollectionCache))
}

object Env {

  lazy val current = "memo" boot new Env(
    lila.common.PlayApp loadConfig "memo",
    lila.db.Env.current)
}
