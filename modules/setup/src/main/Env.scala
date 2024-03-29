package lila.setup

import com.softwaremill.macwire.*
import play.api.Configuration

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    perfsRepo: lila.user.UserPerfsRepo,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
    onStart: lila.round.OnStart,
    gameCache: lila.game.Cached
)(using Executor, akka.stream.Materializer, lila.game.IdGenerator):

  val forms = SetupForm

  val setupForm: lila.hub.setup.SetupForm = SetupForm.api

  val processor = wire[Processor]
