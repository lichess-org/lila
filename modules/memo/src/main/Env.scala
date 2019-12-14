package lila.memo

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._

final class MemoConfig(
    @ConfigName("collection.cache") val cacheColl: CollName,
    @ConfigName("collection.config") val configColl: CollName
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val config = appConfig.get[MemoConfig]("memo")(AutoConfig.loader)

  lazy val mongoCache = wire[MongoCache.Builder]

  lazy val configStore = wire[ConfigStore.Builder]

  lazy val settingStore = wire[SettingStore.Builder]

  lazy val asyncCache = wire[AsyncCache.Builder]
}
