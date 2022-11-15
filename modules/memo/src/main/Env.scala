package lila.memo

import com.softwaremill.macwire.*
import pureconfig._
import pureconfig.generic.derivation.default._
import play.api.{ ConfigLoader, Configuration }

import lila.common.config.*
import com.typesafe.config.Config

case class MemoCollectionConfig(cache: CollName, config: CollName) derives ConfigReader
case class MemoConfig(collection: MemoCollectionConfig, picfit: PicfitConfig) derives ConfigReader

case class PicfitConfig(
    val collection: CollName,
    val endpointGet: String,
    val endpointPost: String,
    val secretKey: Secret
) derives ConfigReader

@Module
final class Env(
    appConfig: Configuration,
    mode: play.api.Mode,
    db: lila.db.Db,
    ws: play.api.libs.ws.StandaloneWSClient
)(using ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem):

  given [A](using ConfigReader[A]): ConfigLoader[A] with
    def load(config: Config, path: String = ""): A =
      summon[ConfigReader[A]].from(ConfigSource.fromConfig(config.getConfig(path)).config())

  private val config = appConfig.get[MemoConfig]("memo")

  lazy val configStore = wire[ConfigStore.Builder]

  lazy val settingStore = wire[SettingStore.Builder]

  lazy val cacheApi = wire[CacheApi]

  lazy val mongoCacheApi = wire[MongoCache.Api]

  lazy val mongoRateLimitApi = wire[MongoRateLimitApi]

  lazy val picfitUrl = new lila.memo.PicfitUrl(config.picfit)

  lazy val picfitApi = new PicfitApi(db(config.picfit.collection), picfitUrl, ws, config.picfit)
