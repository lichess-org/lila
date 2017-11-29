package lila.memo

import com.typesafe.config.Config

final class Env(
    config: Config,
    db: lila.db.Env,
    system: akka.actor.ActorSystem
) {

  private val CollectionCache = config getString "collection.cache"
  private val CollectionConfig = config getString "collection.config"

  private val configColl = db(CollectionConfig)

  lazy val mongoCache: MongoCache.Builder = new MongoCache.Builder(db(CollectionCache))

  lazy val configStore: ConfigStore.Builder = new ConfigStore.Builder(configColl)

  lazy val settingStore: SettingStore.Builder = new SettingStore.Builder(configColl)

  lazy val asyncCache: AsyncCache.Builder = new AsyncCache.Builder()(system)
}

object Env {

  lazy val current = "memo" boot new Env(
    config = lila.common.PlayApp loadConfig "memo",
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system
  )
}
