package lila.pref

import io.methvin.play.autoconfig._
import play.api.Configuration
import scala.concurrent.duration._

import lila.common.config._

case class PrefConfig(
    @ConfigName("collection.pref") prefColl: CollName,
    @ConfigName("cache.ttl") cacheTtl: FiniteDuration
)

final class Env(
    appConfig: Configuration,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {
  private val config = appConfig.get[PrefConfig]("pref")(AutoConfig.loader)

  lazy val api = new PrefApi(db(config.prefColl), asyncCache, config.cacheTtl)
}
