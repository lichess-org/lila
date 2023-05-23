package lila.explorer

import com.softwaremill.macwire.*
import play.api.Configuration

case class InternalEndpoint(value: String) extends AnyVal with StringValue

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    gameImporter: lila.importer.Importer,
    getBotUserIds: lila.user.GetBotIds,
    settingStore: lila.memo.SettingStore.Builder,
    ws: play.api.libs.ws.StandaloneWSClient
)(using
    ec: Executor,
    system: akka.actor.ActorSystem,
    materializer: akka.stream.Materializer
):

  private lazy val internalEndpoint = InternalEndpoint {
    appConfig.get[String]("explorer.internal_endpoint")
  }

  lazy val importer = wire[ExplorerImporter]
