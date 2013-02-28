package lila.app
package setup

import http.Context
import game.{ DbGame, GameRepo, PgnRepo, Pov }
import user.User
import chess.{ Game, Board, Color ⇒ ChessColor }
import ai.Ai
import lobby.{ Hook, Fisherman }
import i18n.I18nDomain
import controllers.routes

import play.api.libs.json.{ Json, JsObject }
import scalaz.effects._

final class Processor(
    userConfigRepo: UserConfigRepo,
    anonConfigRepo: AnonConfigRepo,
    friendConfigMemo: FriendConfigMemo,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    fisherman: Fisherman,
    timelinePush: DbGame ⇒ IO[Unit],
    ai: () ⇒ Ai) extends core.Futuristic {

  def filter(config: FilterConfig)(implicit ctx: Context): IO[Unit] = 
    saveConfig(_ withFilter config)

  def ai(config: AiConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← saveConfig(_ withAi config)
    pov = config.pov
    game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user))
    _ ← gameRepo insert game
    _ ← gameRepo denormalize game
    _ ← timelinePush(game)
    pov2 ← game.player.isHuman.fold(
      io(pov),
      for {
        initialFen ← game.variant.standard.fold(
          io(none[String]),
          gameRepo initialFen game.id)
        pgnString ← pgnRepo get game.id
        aiResult ← { ai().play(game, pgnString, initialFen) map (_.err) }.toIo
        (newChessGame, move) = aiResult
        (progress, pgn) = game.update(newChessGame, move)
        _ ← gameRepo save progress
        _ ← pgnRepo.save(game.id, pgn)
      } yield pov withGame progress.game
    )
  } yield pov2

  def friend(config: FriendConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← saveConfig(_ withFriend config)
    pov = config.pov
    game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user))
    _ ← gameRepo insert game
    _ ← gameRepo denormalize game // to get the initialFen before the game starts
    _ ← timelinePush(game)
    _ ← friendConfigMemo.set(pov.game.id, config)
  } yield pov

  def hook(config: HookConfig)(implicit ctx: Context): IO[Hook] = for {
    _ ← saveConfig(_ withHook config)
    hook = config hook ctx.me
    _ ← fisherman add hook
  } yield hook

  def api(implicit ctx: Context): IO[JsObject] = {
    val domain = "http://" + I18nDomain(ctx.req.domain).commonDomain
    val config = ApiConfig
    val pov = config.pov
    val game = ctx.me.fold(pov.game)(user ⇒ pov.game.updatePlayer(pov.color, _ withUser user)).start
    import chess.Color._
    for {
      _ ← gameRepo insert game
      _ ← timelinePush(game)
    } yield Json.obj(
      White.name -> (domain + routes.Round.player(game fullIdOf White).url),
      Black.name -> (domain + routes.Round.player(game fullIdOf Black).url)
    )
  }

  private def saveConfig(map: UserConfig ⇒ UserConfig)(implicit ctx: Context): IO[Unit] =
    ctx.me.fold(anonConfigRepo.update(ctx.req) _)(user ⇒ userConfigRepo.update(user) _)(map)

}
