package lila.setup

import com.softwaremill.macwire.*
import play.api.Configuration

import lila.oauth.OAuthServer

@Module
@annotation.nowarn("msg=unused")
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached,
    oauthServer: OAuthServer
)(using Executor, akka.stream.Materializer, lila.game.IdGenerator):

  lazy val forms = SetupForm

  lazy val processor = wire[Processor]

  lazy val bulk = wire[SetupBulkApi]
