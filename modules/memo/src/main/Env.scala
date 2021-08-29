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
    mode: play.api.Mode,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val config = appConfig.get[MemoConfig]("memo")(AutoConfig.loader)

  lazy val configStore = wire[ConfigStore.Builder]

  lazy val settingStore = wire[SettingStore.Builder]

  lazy val cacheApi = wire[CacheApi]

  lazy val mongoCacheApi = wire[MongoCache.Api]

  lazy val mongoRateLimitApi = wire[MongoRateLimitApi]
}
