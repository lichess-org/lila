package lila.setup

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._
import lila.oauth.OAuthServer

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    fishnetPlayer: lila.fishnet.Player,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached,
    oauthServer: OAuthServer
)(implicit ec: scala.concurrent.ExecutionContext, mat: akka.stream.Materializer) {

  private lazy val maxPlaying = appConfig.get[Max]("setup.max_playing")

  lazy val forms = SetupForm

  lazy val processor = wire[Processor]

  lazy val bulk = wire[BulkChallengeApi]
}
