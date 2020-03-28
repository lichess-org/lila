package lidraughts.setup

import lidraughts.game.{ GameRepo, Pov, PerfPicker }
import lidraughts.lobby.actorApi.{ AddHook, AddSeek }
import lidraughts.user.{ User, UserContext }

private[setup] final class Processor(
    bus: lidraughts.common.Bus,
    gameCache: lidraughts.game.Cached,
    maxPlaying: Int,
    draughtsnetPlayer: lidraughts.draughtsnet.Player,
    anonConfigRepo: AnonConfigRepo,
    userConfigRepo: UserConfigRepo,
    onStart: String => Unit
) {

  def filter(config: FilterConfig)(implicit ctx: UserContext): Funit =
    saveConfig(_ withFilter config)

  def ai(config: AiConfig)(implicit ctx: UserContext): Fu[Pov] = {
    val pov = config pov ctx.me
    saveConfig(_ withAi config) >>
      (GameRepo insertDenormalized pov.game) >>-
      onStart(pov.gameId) >> {
        pov.game.player.isAi ?? draughtsnetPlayer(pov.game)
      } inject pov
  }

  def hook(
    configBase: HookConfig,
    uid: lidraughts.socket.Socket.Uid,
    sid: Option[String],
    blocking: Set[String]
  )(implicit ctx: UserContext): Fu[Processor.HookResult] = {
    import Processor.HookResult._
    val config = configBase.fixColor
    saveConfig(_ withHook config) >> {
      config.hook(uid, ctx.me, sid, blocking) match {
        case Left(hook) => fuccess {
          bus.publish(AddHook(hook), 'lobbyTrouper)
          Created(hook.id)
        }
        case Right(Some(seek)) => ctx.userId.??(gameCache.nbPlaying) map { nbPlaying =>
          if (nbPlaying >= maxPlaying) Refused
          else {
            bus.publish(AddSeek(seek), 'lobbyTrouper)
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
