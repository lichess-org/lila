package lila.local

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.{ *, given }
import lila.core.config.*

@Module
final private class LocalConfig(
    @ConfigName("asset_path") val assetPath: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    getFile: String => java.io.File
)(using Executor, akka.stream.Materializer):

  private val config: LocalConfig = appConfig.get[LocalConfig]("local")(AutoConfig.loader)

  val repo = LocalRepo(db(CollName("local_bot")), db(CollName("local_asset")))

  val api: LocalApi = wire[LocalApi]
