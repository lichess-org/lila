package lila.setup

import lila.game.{ Game, GameRepo, PgnRepo, Pov }
import lila.user.{ User, Context }
import chess.{ Game ⇒ ChessGame, Board, Color ⇒ ChessColor }
import lila.ai.Ai
import lila.lobby.{ Hook, Fisherman }
import lila.i18n.I18nDomain
import lila.hub.actorApi.router.Player
import makeTimeout.short

import tube.{ userConfigTube, anonConfigTube }
import lila.game.tube.gameTube
import lila.db.api._

import play.api.libs.json.{ Json, JsObject }
import akka.actor.ActorRef
import akka.pattern.ask

private[setup] final class Processor(
    friendConfigMemo: FriendConfigMemo,
    fisherman: Fisherman,
    timeline: ActorRef,
    router: ActorRef,
    ai: Ai) {

  def filter(config: FilterConfig)(implicit ctx: Context): Funit =
    saveConfig(_ withFilter config)

  def ai(config: AiConfig)(implicit ctx: Context): Fu[Pov] = {
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user))
    saveConfig(_ withAi config) >>
      $insert(game) >>
      (GameRepo denormalize game) >>
      (timeline ! game) >>
      game.player.isHuman.fold(fuccess(pov), for {
        initialFen ← game.variant.standard ?? (GameRepo initialFen game.id)
        pgnString ← PgnRepo get game.id
        aiResult ← ai.play(game.toChess, pgnString, initialFen, ~game.aiLevel) map (_.err)
        (newChessGame, move) = aiResult
        (progress, pgn) = game.update(newChessGame, move)
        _ ← (GameRepo save progress) >> PgnRepo.save(game.id, pgn)
      } yield pov withGame progress.game)
  }

  def friend(config: FriendConfig)(implicit ctx: Context): Fu[Pov] = {
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user))
    saveConfig(_ withFriend config) >>
      $insert(game) >>
      // to get the initialFen before the game starts
      (GameRepo denormalize game) >>
      friendConfigMemo.set(pov.game.id, config) inject pov
  }

  def hook(config: HookConfig)(implicit ctx: Context): Fu[Hook] = {
    val hook = config hook ctx.me
    saveConfig(_ withHook config) >> (fisherman add hook) inject hook
  }

  def api(implicit ctx: Context): Fu[JsObject] = {
    val config = ApiConfig
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user)).start
    import ChessColor.{ White, Black }
    $insert(game) zip
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
