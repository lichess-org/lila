package lila.setup

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.common.config.*
import lila.oauth.OAuthServer

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached,
    oauthServer: OAuthServer
)(using
    ec: scala.concurrent.ExecutionContext,
    mat: akka.stream.Materializer,
    idGenerator: lila.game.IdGenerator
):

  lazy val forms = SetupForm

  lazy val processor = wire[Processor]

  lazy val bulk = wire[SetupBulkApi]
