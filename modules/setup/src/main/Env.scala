package lila.setup

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    fishnetPlayer: lila.fishnet.Player,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val maxPlaying = appConfig.get[Max]("setup.max_playing")

  lazy val forms = wire[SetupForm]

  lazy val processor = wire[Processor]
}
