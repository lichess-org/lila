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
    getFile: (String => java.io.File)
)(using
    Executor,
    akka.stream.Materializer
)(using mode: play.api.Mode, scheduler: Scheduler):

  private val config: LocalConfig = appConfig.get[LocalConfig]("local")(AutoConfig.loader)

  val repo = LocalRepo(db(CollName("local_bots")), db(CollName("local_assets")))

  val api: LocalApi = wire[LocalApi]
