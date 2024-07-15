package lila.memo

import com.softwaremill.macwire.*
import play.api.{ ConfigLoader, Configuration }

import lila.common.autoconfig.*
import lila.common.autoconfig.given
import lila.common.config.given
import lila.core.config.*

final class MemoConfig(
    @ConfigName("collection.cache") val cacheColl: CollName,
    @ConfigName("collection.config") val configColl: CollName,
    val picfit: PicfitConfig
)

final class PicfitConfig(
    val collection: CollName,
    val endpointGet: EndpointUrl,
    val endpointPost: EndpointUrl,
    val secretKey: Secret
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor, Scheduler, play.api.Mode, lila.core.config.RateLimit):

  given ConfigLoader[PicfitConfig] = AutoConfig.loader
  private val config               = appConfig.get[MemoConfig]("memo")(AutoConfig.loader)

  val cacheApi = wire[CacheApi]

  val configStore = wire[ConfigStore.Builder]

  val settingStore = wire[SettingStore.Builder]

  val mongoCacheApi = wire[MongoCache.Api]

  val mongoRateLimitApi = wire[MongoRateLimitApi]

  val picfitUrl = lila.memo.PicfitUrl(config.picfit)

  val picfitApi = PicfitApi(db(config.picfit.collection), picfitUrl, ws, config.picfit)
