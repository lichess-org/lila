package lila.setup

import lila.common.Bus
import lila.game.{ GameRepo, IdGenerator, Pov }
import lila.lobby.actorApi.{ AddHook, AddSeek }
import lila.lobby.Seek
import lila.user.{ User, UserContext }

final private[setup] class Processor(
    gameCache: lila.game.Cached,
    gameRepo: GameRepo,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
    onStart: lila.round.OnStart
)(using ec: Executor, idGenerator: IdGenerator):

  def ai(config: AiConfig)(using ctx: UserContext): Fu[Pov] = for
    pov <- config pov ctx.me
    _   <- gameRepo insertDenormalized pov.game
    _ = onStart(pov.gameId)
    _ <- pov.game.player.isAi ?? fishnetPlayer(pov.game)
  yield pov

  def apiAi(config: ApiAiConfig, me: User): Fu[Pov] = for
    pov <- config pov me.some
    _   <- gameRepo insertDenormalized pov.game
    _ = onStart(pov.gameId)
    _ <- pov.game.player.isAi ?? fishnetPlayer(pov.game)
  yield pov

  def hook(
      me: Option[User],
      configBase: HookConfig,
      sri: lila.socket.Socket.Sri,
      sid: Option[String],
      blocking: lila.pool.Blocking
  ): Fu[Processor.HookResult] =
    import Processor.HookResult.*
    val config = configBase.fixColor
    config.hook(sri, me, sid, blocking) match
      case Left(hook) =>
        fuccess:
          Bus.publish(AddHook(hook), "lobbyActor")
          Created(hook.id)
      case Right(Some(seek)) =>
        me.fold(fuccess(Refused)): user =>
          createSeekIfAllowed(seek, user.id)
      case _ => fuccess(Refused)

  def createSeekIfAllowed(seek: Seek, userId: UserId): Fu[Processor.HookResult] =
    gameCache.nbPlaying(userId) map { nbPlaying =>
      import Processor.HookResult.*
      if (nbPlaying >= lila.game.Game.maxPlaying) Refused
      else
        Bus.publish(AddSeek(seek), "lobbyActor")
        Created(seek.id)
    }

object Processor:

  enum HookResult:
    case Created(id: String)
    case Refused
