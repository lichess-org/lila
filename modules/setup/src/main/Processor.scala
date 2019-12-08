package lila.setup

import lila.common.Bus
import lila.common.config.Max
import lila.game.Pov
import lila.lobby.actorApi.{ AddHook, AddSeek }
import lila.user.UserContext

private[setup] final class Processor(
    gameCache: lila.game.Cached,
    gameRepo: lila.game.GameRepo,
    maxPlaying: Max,
    fishnetPlayer: lila.fishnet.Player,
    anonConfigRepo: AnonConfigRepo,
    userConfigRepo: UserConfigRepo,
    onStart: lila.round.OnStart
) {

  def filter(config: FilterConfig)(implicit ctx: UserContext): Funit =
    saveConfig(_ withFilter config)

  def ai(config: AiConfig)(implicit ctx: UserContext): Fu[Pov] = {
    val pov = config pov ctx.me
    saveConfig(_ withAi config) >>
      (gameRepo insertDenormalized pov.game) >>-
      onStart(pov.gameId) >> {
        pov.game.player.isAi ?? fishnetPlayer(pov.game)
      } inject pov
  }

  def hook(
    configBase: HookConfig,
    sri: lila.socket.Socket.Sri,
    sid: Option[String],
    blocking: Set[String]
  )(implicit ctx: UserContext): Fu[Processor.HookResult] = {
    import Processor.HookResult._
    val config = configBase.fixColor
    saveConfig(_ withHook config) >> {
      config.hook(sri, ctx.me, sid, blocking) match {
        case Left(hook) => fuccess {
          Bus.publish(AddHook(hook), "lobbyTrouper")
          Created(hook.id)
        }
        case Right(Some(seek)) => ctx.userId.??(gameCache.nbPlaying) map { nbPlaying =>
          if (maxPlaying <= nbPlaying) Refused
          else {
            Bus.publish(AddSeek(seek), "lobbyTrouper")
            Created(seek.id)
          }
        }
        case _ => fuccess(Refused)
      }
    }
  }

  def saveFriendConfig(config: FriendConfig)(implicit ctx: UserContext) =
    saveConfig(_ withFriend config)

  def saveHookConfig(config: HookConfig)(implicit ctx: UserContext) =
    saveConfig(_ withHook config)

  private def saveConfig(map: UserConfig => UserConfig)(implicit ctx: UserContext): Funit =
    ctx.me.fold(anonConfigRepo.update(ctx.req) _)(user => userConfigRepo.update(user) _)(map)
}

object Processor {

  sealed trait HookResult
  object HookResult {
    case class Created(id: String) extends HookResult
    case object Refused extends HookResult
  }
}
