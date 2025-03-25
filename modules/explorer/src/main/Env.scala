package lila.explorer

import com.softwaremill.macwire.*
import play.api.Configuration

case class InternalEndpoint(value: String) extends AnyVal with StringValue

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    gameImporter: lila.game.importer.Importer,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor):

  private val internalEndpoint = InternalEndpoint:
    appConfig.get[String]("explorer.internal_endpoint")

  val importer = wire[ExplorerImporter]
