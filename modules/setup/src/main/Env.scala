package lila.setup

import com.softwaremill.macwire.*
import play.api.Configuration

@Module
final class Env(
    appConfig: Configuration,
    gameRepo: lila.game.GameRepo,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart,
    gameCache: lila.game.Cached
)(using Executor, akka.stream.Materializer, lila.game.IdGenerator):

  val forms = SetupForm

  val setupForm: lila.core.setup.SetupForm = SetupForm.api

  val processor = wire[Processor]
