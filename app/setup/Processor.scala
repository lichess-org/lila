package lila
package setup

import http.Context
import game.{ DbGame, GameRepo, PgnRepo, Pov }
import user.User
import chess.{ Game, Board, Color ⇒ ChessColor }
import ai.Ai
import lobby.{ Hook, Fisherman }
import i18n.I18nDomain
import controllers.routes

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
    game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _ withUser user),
      pov.game)
    _ ← gameRepo insert game
    _ ← gameRepo denormalizeStarted game
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
    game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _ withUser user),
      pov.game)
    _ ← gameRepo insert game
    _ ← timelinePush(game)
    _ ← friendConfigMemo.set(pov.game.id, config)
  } yield pov

  def hook(config: HookConfig)(implicit ctx: Context): IO[Hook] = for {
    _ ← saveConfig(_ withHook config)
    hook = config hook ctx.me
    _ ← fisherman add hook
  } yield hook

  def api(implicit ctx: Context): IO[Map[String, Any]] = {
    val domain = "http://" + I18nDomain(ctx.req.domain).commonDomain
    val config = ApiConfig
    val pov = config.pov
    val game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _ withUser user),
      pov.game).start
    for {
      _ ← gameRepo insert game
      _ ← timelinePush(game)
    } yield ChessColor.all map { color ⇒
      color.name -> (domain + routes.Round.player(game fullIdOf color).url)
    } toMap
  }

  private def saveConfig(map: UserConfig ⇒ UserConfig)(implicit ctx: Context): IO[Unit] =
    ctx.me.fold(
      user ⇒ userConfigRepo.update(user) _,
      anonConfigRepo.update(ctx.req) _
    )(map)

}
