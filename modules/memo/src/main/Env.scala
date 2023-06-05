package lila.memo

import com.softwaremill.macwire.*
import lila.common.autoconfig.{ *, given }
import play.api.{ ConfigLoader, Configuration }

import lila.common.config.*

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
    mode: play.api.Mode,
    db: lila.db.Db,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor, Scheduler):

  given ConfigLoader[PicfitConfig] = AutoConfig.loader
  private val config               = appConfig.get[MemoConfig]("memo")(AutoConfig.loader)

  lazy val configStore = wire[ConfigStore.Builder]

  lazy val settingStore = wire[SettingStore.Builder]

  lazy val cacheApi = wire[CacheApi]

  lazy val mongoCacheApi = wire[MongoCache.Api]

  lazy val mongoRateLimitApi = wire[MongoRateLimitApi]

  lazy val picfitUrl = lila.memo.PicfitUrl(config.picfit)

  lazy val picfitApi = PicfitApi(db(config.picfit.collection), picfitUrl, ws, config.picfit)
