package lila.setup

import akka.actor.ActorSelection
import akka.pattern.ask
import chess.{ Game ⇒ ChessGame, Board, Color ⇒ ChessColor }
import play.api.libs.json.{ Json, JsObject }

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ Game, GameRepo, Pov, Progress }
import lila.hub.actorApi.router.Player
import lila.i18n.I18nDomain
import lila.lobby.actorApi.AddHook
import lila.lobby.Hook
import lila.user.{ User, Context }
import makeTimeout.short
import tube.{ userConfigTube, anonConfigTube }

private[setup] final class Processor(
    lobby: ActorSelection,
    friendConfigMemo: FriendConfigMemo,
    timeline: ActorSelection,
    router: ActorSelection,
    aiPlay: Game ⇒ Fu[Progress]) {

  def filter(config: FilterConfig)(implicit ctx: Context): Funit =
    saveConfig(_ withFilter config)

  def ai(config: AiConfig)(implicit ctx: Context): Fu[Pov] = {
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user))
    saveConfig(_ withAi config) >>
      (GameRepo insertDenormalized game) >>-
      (timeline ! game) >>
      game.player.isHuman.fold(
        fuccess(pov),
        aiPlay(game) map { progress ⇒ pov withGame progress.game }
      )
  }

  def friend(config: FriendConfig)(implicit ctx: Context): Fu[Pov] = {
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user))
    saveConfig(_ withFriend config) >>
      (GameRepo insertDenormalized game) >>-
      friendConfigMemo.set(pov.game.id, config) inject pov
  }

  def hook(
    config: HookConfig,
    uid: String,
    sid: Option[String])(implicit ctx: Context): Funit =
    saveConfig(_ withHook config) >>- {
      lobby ! AddHook(config.hook(uid, ctx.me, sid))
    }

  def api(implicit ctx: Context): Fu[JsObject] = {
    val config = ApiConfig
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user)).start
    import ChessColor.{ White, Black }
    $insert bson game zip
      router ? Player(game fullIdOf White) zip
      router ? Player(game fullIdOf Black) collect {
        case (_, (whiteUrl: String, blackUrl: String)) ⇒ Json.obj(
          White.name -> whiteUrl,
          Black.name -> blackUrl)
      }
  }

  private def saveConfig(map: UserConfig ⇒ UserConfig)(implicit ctx: Context): Funit =
    ctx.me.fold(AnonConfigRepo.update(ctx.req) _)(user ⇒ UserConfigRepo.update(user) _)(map)
}
