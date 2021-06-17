package lila.setup

import lila.common.Bus
import lila.common.config.Max
import lila.game.Pov
import lila.lobby.actorApi.{ AddHook, AddSeek }
import lila.lobby.Seek
import lila.user.{ User, UserContext }

final private[setup] class Processor(
    gameCache: lila.game.Cached,
    gameRepo: lila.game.GameRepo,
    maxPlaying: Max,
    fishnetPlayer: lila.fishnet.FishnetPlayer,
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
        ctx.userId match {
          case None         => fuccess(Refused)
          case Some(userId) => createSeekIfAllowed(seek, userId)
        }
      case _ => fuccess(Refused)
    }
  }

  def createSeekIfAllowed(seek: Seek, userId: User.ID): Fu[Processor.HookResult] =
    gameCache.nbPlaying(userId) map { nbPlaying =>
      import Processor.HookResult._
      if (maxPlaying <= nbPlaying) Refused
      else {
        Bus.publish(AddSeek(seek), "lobbyTrouper")
        Created(seek.id)
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
