package lila.setup

import lila.common.Bus
import lila.common.config.Max
import lila.game.Pov
import lila.lobby.actorApi.{ AddHook, AddSeek }
import lila.user.{ User, UserContext }

final private[setup] class Processor(
    gameCache: lila.game.Cached,
    gameRepo: lila.game.GameRepo,
    maxPlaying: Max,
    fishnetPlayer: lila.fishnet.Player,
    onStart: lila.round.OnStart
)(implicit ec: scala.concurrent.ExecutionContext) {

  def ai(config: AiConfig)(implicit ctx: UserContext): Fu[Pov] = {
    val pov = config pov ctx.me
    (gameRepo insertDenormalized pov.game) >>-
      onStart(pov.gameId) >> {
      pov.game.player.isAi ?? fishnetPlayer(pov.game)
    } inject pov
  }

  def apiAi(config: ApiAiConfig, me: User): Fu[Pov] = {
    val pov = config pov me.some
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
    config.hook(sri, ctx.me, sid, blocking) match {
      case Left(hook) =>
        fuccess {
          Bus.publish(AddHook(hook), "lobbyTrouper")
          Created(hook.id)
        }
      case Right(Some(seek)) =>
        ctx.userId.??(gameCache.nbPlaying) dmap { nbPlaying =>
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

object Processor {

  sealed trait HookResult
  object HookResult {
    case class Created(id: String) extends HookResult
    case object Refused            extends HookResult
  }
}
