package lila.jsBot

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.autoconfig.*
import lila.core.config.*
import lila.common.config.GetRelativeFile

@Module
final private class JsBotConfig(
    @ConfigName("asset_path") val assetPath: String
)

@Module
final class Env(
    appConfig: Configuration,
    db: lila.db.Db,
    getFile: GetRelativeFile
)(using Executor, akka.stream.Materializer):

  private val config: JsBotConfig = appConfig.get[JsBotConfig]("jsBot")(using AutoConfig.loader)

  val repo = JsBotRepo(db(CollName("jsbot")), db(CollName("jsbot_asset")))

  val api: JsBotApi = wire[JsBotApi]
