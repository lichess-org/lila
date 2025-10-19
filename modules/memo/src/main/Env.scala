package lila.memo

import com.softwaremill.macwire.*
import play.api.{ ConfigLoader, Configuration }

import lila.common.autoconfig.{ *, given }
import lila.common.config.given
import lila.core.config.*

final class MemoConfig(
    @ConfigName("collection.cache") val cacheColl: CollName,
    @ConfigName("collection.config") val configColl: CollName,
    val picfit: PicfitConfig,
    val cloudflare: CloudflareConfig
)

final class PicfitConfig(
    val collection: CollName,
    val endpointGet: String,
    val endpointPost: String,
    val secretKey: Secret
)

final class CloudflareConfig(
    @ConfigName("zone_id") val zoneId: String,
    @ConfigName("api_token") val apiToken: Secret
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    ws: play.api.libs.ws.StandaloneWSClient,
    net: NetConfig
)(using Executor, Scheduler, play.api.Mode):

  export net.{ domain, assetDomain }

  given ConfigLoader[PicfitConfig] = AutoConfig.loader
  given ConfigLoader[CloudflareConfig] = AutoConfig.loader
  val config = appConfig.get[MemoConfig]("memo")(using AutoConfig.loader)
  private val picfitColl = db(config.picfit.collection)
  private val picfitConfig = config.picfit
  private val cloudflareConfig = config.cloudflare

  val imageGetOrigin = ImageGetOrigin:
    val pathBegin = config.picfit.endpointGet.indexOf('/', 8)
    if pathBegin == -1 then config.picfit.endpointGet else config.picfit.endpointGet.slice(0, pathBegin)

  val cacheApi = wire[CacheApi]

  val settingStore = wire[SettingStore.Builder]

  val mongoCacheApi = wire[MongoCache.Api]

  val mongoRateLimitApi = wire[MongoRateLimitApi]

  private val cloudflareApi = wire[CloudflareApi]

  val picfitUrl = wire[PicfitUrl]

  val picfitApi = wire[PicfitApi]

  val markdown = wire[MarkdownCache]

  lila.common.Cli.handle:
    case "cache" :: "clear" :: name :: Nil =>
      cacheApi.clearByName(name) match
        case Some(nb) => fuccess(s"Cleared $nb entries from cache $name")
        case None => fufail(s"No cache named $name")
