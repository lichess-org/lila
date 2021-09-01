package lila.memo

import com.softwaremill.macwire._
import io.methvin.play.autoconfig._
import play.api.Configuration

import lila.common.config._

final class MemoConfig(
    @ConfigName("collection.cache") val cacheColl: CollName,
    @ConfigName("collection.config") val configColl: CollName,
    val picfit: PicfitConfig
)

final class PicfitConfig(
    val collection: CollName,
    val endpointGet: String,
    val endpointPost: String,
    val secretKey: Secret
)

@Module
final class Env(
    appConfig: Configuration,
    mode: play.api.Mode,
    db: lila.db.Db,
    ws: play.api.libs.ws.StandaloneWSClient
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  implicit val picfitLoader = AutoConfig.loader[PicfitConfig]
  private val config        = appConfig.get[MemoConfig]("memo")(AutoConfig.loader)

  lazy val configStore = wire[ConfigStore.Builder]

  lazy val settingStore = wire[SettingStore.Builder]

  lazy val cacheApi = wire[CacheApi]

  lazy val mongoCacheApi = wire[MongoCache.Api]

  lazy val mongoRateLimitApi = wire[MongoRateLimitApi]

  lazy val picfitApi = new PicfitApi(db(config.picfit.collection), ws, config.picfit)
}
