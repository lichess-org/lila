package lila.setup

import lila.common.Bus
import lila.core.perf.UserWithPerfs
import lila.lobby.{ AddHook, AddSeek, Seek }

final private[setup] class Processor(
    gameApi: lila.core.game.GameApi,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi,
    onStart: lila.core.game.OnStart
)(using Executor, lila.core.game.IdGenerator, lila.core.game.NewPlayer):

  def ai(config: AiConfig)(using me: Option[Me]): Fu[Pov] = for
    me  <- me.map(_.value).soFu(userApi.withPerf(_, config.perfType))
    pov <- config.pov(me)
    _   <- gameRepo.insertDenormalized(pov.game)
    _ = onStart.exec(pov.gameId)
  yield pov

  def apiAi(config: ApiAiConfig)(using me: Me): Fu[Pov] = for
    me  <- userApi.withPerf(me, config.perfType)
    pov <- config.pov(me.some)
    _   <- gameRepo.insertDenormalized(pov.game)
    _ = onStart.exec(pov.gameId)
  yield pov

  def hook(
      config: HookConfig,
      sri: lila.core.socket.Sri,
      sid: Option[String],
      blocking: lila.core.pool.Blocking
  )(using me: Option[UserWithPerfs]): Fu[Processor.HookResult] =
    import Processor.HookResult.*
    config.hook(sri, me, sid, blocking) match
      case Left(hook) =>
        fuccess:
          Bus.publish(AddHook(hook), "lobbyActor")
          Created(hook.id)
      case Right(Some(seek)) => me.fold(fuccess(Refused))(u => createSeekIfAllowed(seek, u.id))
      case _                 => fuccess(Refused)

  def createSeekIfAllowed(seek: Seek, owner: UserId): Fu[Processor.HookResult] =
    gameApi.nbPlaying(owner).map { nbPlaying =>
      import Processor.HookResult.*
      if lila.core.game.maxPlaying <= nbPlaying
      then Refused
      else
        Bus.publish(AddSeek(seek), "lobbyActor")
        Created(seek.id)
    }

object Processor:

  enum HookResult:
    case Created(id: String)
    case Refused
