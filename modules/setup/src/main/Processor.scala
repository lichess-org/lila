package lila.setup

import lila.common.Bus
import lila.game.{ GameRepo, IdGenerator, Pov }
import lila.lobby.actorApi.{ AddHook, AddSeek }
import lila.lobby.Seek
import lila.user.{ Me, User, UserPerfsRepo }

final private[setup] class Processor(
    gameCache: lila.game.Cached,
    gameRepo: GameRepo,
    perfsRepo: UserPerfsRepo,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
    onStart: lila.round.OnStart
)(using Executor, IdGenerator):

  def ai(config: AiConfig)(using me: Option[Me]): Fu[Pov] = for
    me  <- me.map(_.value).soFu(perfsRepo.withPerf(_, config.perfType))
    pov <- config pov me
    _   <- gameRepo insertDenormalized pov.game
    _ = onStart(pov.gameId)
    _ <- pov.game.player.isAi so fishnetPlayer(pov.game)
  yield pov

  def apiAi(config: ApiAiConfig)(using me: Me): Fu[Pov] = for
    me  <- perfsRepo.withPerf(me, config.perfType)
    pov <- config pov me.some
    _   <- gameRepo insertDenormalized pov.game
    _ = onStart(pov.gameId)
    _ <- pov.game.player.isAi so fishnetPlayer(pov.game)
  yield pov

  def hook(
      configBase: HookConfig,
      sri: lila.socket.Socket.Sri,
      sid: Option[String],
      blocking: lila.pool.Blocking
  )(using me: Option[User.WithPerfs]): Fu[Processor.HookResult] =
    import Processor.HookResult.*
    val config = configBase.fixColor
    config.hook(sri, me, sid, blocking) match
      case Left(hook) =>
        fuccess:
          Bus.publish(AddHook(hook), "lobbyActor")
          Created(hook.id)
      case Right(Some(seek)) => me.fold(fuccess(Refused))(u => createSeekIfAllowed(seek, u.id))
      case _                 => fuccess(Refused)

  def createSeekIfAllowed(seek: Seek, owner: UserId): Fu[Processor.HookResult] =
    gameCache.nbPlaying(owner) map { nbPlaying =>
      import Processor.HookResult.*
      if nbPlaying >= lila.game.Game.maxPlaying
      then Refused
      else
        Bus.publish(AddSeek(seek), "lobbyActor")
        Created(seek.id)
    }

object Processor:

  enum HookResult:
    case Created(id: String)
    case Refused
