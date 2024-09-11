package lila.setup

import com.softwaremill.macwire.*

@Module
final class Env(
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart,
    gameApi: lila.core.game.GameApi
)(using Executor, akka.stream.Materializer, lila.core.game.IdGenerator, lila.core.game.NewPlayer):

  val forms = SetupForm

  val setupForm: lila.core.setup.SetupForm = SetupForm.api

  val processor = wire[Processor]
