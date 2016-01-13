package lila.setup

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.{ Game => ChessGame, Board, Color => ChessColor }
import play.api.libs.json.{ Json, JsObject }

import lila.db.api._
import lila.game.{ Game, GameRepo, Pov, Progress, PerfPicker }
import lila.i18n.I18nDomain
import lila.lobby.actorApi.{ AddHook, AddSeek }
import lila.lobby.Hook
import lila.user.{ User, UserContext }
import makeTimeout.short
import tube.{ userConfigTube, anonConfigTube }

private[setup] final class Processor(
    lobby: ActorSelection,
    friendConfigMemo: FriendConfigMemo,
    router: ActorSelection,
    onStart: String => Unit,
    aiPlay: Game => Fu[Progress]) {

  def filter(config: FilterConfig)(implicit ctx: UserContext): Funit =
    saveConfig(_ withFilter config)

  def ai(config: AiConfig)(implicit ctx: UserContext): Fu[Pov] = {
    val pov = blamePov(config.pov, ctx.me)
    saveConfig(_ withAi config) >>
      (GameRepo insertDenormalized pov.game) >>-
      onStart(pov.game.id) >>
      pov.game.player.isHuman.fold(
        fuccess(pov),
        aiPlay(pov.game) map { progress => pov withGame progress.game }
      )
  }

  def friend(config: FriendConfig)(implicit ctx: UserContext): Fu[Pov] = {
    val pov = blamePov(config.pov, ctx.me)
    saveConfig(_ withFriend config) >>
      (GameRepo.insertDenormalized(pov.game, ratedCheck = false)) >>-
      friendConfigMemo.set(pov.game.id, config) inject pov
  }

  private def blamePov(pov: Pov, user: Option[User]): Pov = pov withGame {
    user.fold(pov.game) { u =>
      pov.game.updatePlayer(pov.color, _.withUser(u.id, PerfPicker.mainOrDefault(pov.game)(u.perfs)))
    }
  }

  def hook(
    configBase: HookConfig,
    uid: String,
    sid: Option[String],
    blocking: Set[String])(implicit ctx: UserContext): Fu[String] = {
    val config = configBase.fixColor
    saveConfig(_ withHook config) >> {
      config.hook(uid, ctx.me, sid, blocking) match {
        case Left(hook) => fuccess {
          lobby ! AddHook(hook)
          hook.id
        }
        case Right(Some(seek)) => fuccess {
          lobby ! AddSeek(seek)
          seek.id
        }
        case Right(None) if ctx.me.isEmpty => fufail(new IllegalArgumentException("Anon can't create seek"))
        case _                             => fufail("Can't create seek for some unknown reason")
      }
    }
  }

  private def saveConfig(map: UserConfig => UserConfig)(implicit ctx: UserContext): Funit =
    ctx.me.fold(AnonConfigRepo.update(ctx.req) _)(user => UserConfigRepo.update(user) _)(map)
}
