package lila.memo

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

case class MemoConfig(
    @ConfigName("collection.cache") cacheColl: String,
    @ConfigName("collection.config") configColl: String
)

final class Env(
    appConfig: Configuration,
    db: lila.db.Env,
    system: akka.actor.ActorSystem
) {

  private val config = appConfig.get[MemoConfig]("memo")(AutoConfig.loader)

  private val configColl = db(config.configColl)

  lazy val mongoCache: MongoCache.Builder = new MongoCache.Builder(db(config.cacheColl))

  lazy val configStore: ConfigStore.Builder = new ConfigStore.Builder(configColl)

  lazy val settingStore: SettingStore.Builder = new SettingStore.Builder(configColl)

  lazy val asyncCache: AsyncCache.Builder = new AsyncCache.Builder()(system)
}
