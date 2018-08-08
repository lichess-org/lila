package lidraughts.setup

import akka.actor.ActorSelection

import lidraughts.game.{ GameRepo, Pov, PerfPicker }
import lidraughts.lobby.actorApi.{ AddHook, AddSeek }
import lidraughts.user.{ User, UserContext }

private[setup] final class Processor(
    lobby: ActorSelection,
    gameCache: lidraughts.game.Cached,
    maxPlaying: Int,
    onStart: String => Unit
) {

  def filter(config: FilterConfig)(implicit ctx: UserContext): Funit =
    saveConfig(_ withFilter config)

  def hook(
    configBase: HookConfig,
    uid: String,
    sid: Option[String],
    blocking: Set[String]
  )(implicit ctx: UserContext): Fu[Processor.HookResult] = {
    import Processor.HookResult._
    val config = configBase.fixColor
    saveConfig(_ withHook config) >> {
      config.hook(uid, ctx.me, sid, blocking) match {
        case Left(hook) => fuccess {
          lobby ! AddHook(hook)
          Created(hook.id)
        }
        case Right(Some(seek)) => ctx.userId.??(gameCache.nbPlaying) map { nbPlaying =>
          if (nbPlaying >= maxPlaying) Refused
          else {
            lobby ! AddSeek(seek)
            Created(seek.id)
          }
        }
        case Right(None) if ctx.me.isEmpty => fuccess(Refused)
        case _ => fuccess(Refused)
      }
    }
  }

  def saveFriendConfig(config: FriendConfig)(implicit ctx: UserContext) =
    saveConfig(_ withFriend config)

  def saveHookConfig(config: HookConfig)(implicit ctx: UserContext) =
    saveConfig(_ withHook config)

  private def saveConfig(map: UserConfig => UserConfig)(implicit ctx: UserContext): Funit =
    ctx.me.fold(AnonConfigRepo.update(ctx.req) _)(user => UserConfigRepo.update(user) _)(map)
}

object Processor {

  sealed trait HookResult
  object HookResult {
    case class Created(id: String) extends HookResult
    case object Refused extends HookResult
  }
}
