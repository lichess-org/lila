package lila.explorer

import com.softwaremill.macwire.*
import play.api.Configuration

case class InternalEndpoint(value: String) extends AnyVal with StringValue

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    gameImporter: lila.importer.Importer,
    ws: play.api.libs.ws.StandaloneWSClient
)(using Executor):

  private lazy val internalEndpoint = InternalEndpoint {
    appConfig.get[String]("explorer.internal_endpoint")
  }

  lazy val importer = wire[ExplorerImporter]
