package lila
package setup

import http.Context
import game.{ DbGame, GameRepo, Pov }
import user.User
import chess.{ Game, Board }
import ai.Ai
import lobby.{ Hook, Fisherman }

import com.mongodb.DBRef
import scalaz.effects._

final class Processor(
    configRepo: UserConfigRepo,
    friendConfigMemo: FriendConfigMemo,
    gameRepo: GameRepo,
    fisherman: Fisherman,
    timelinePush: DbGame ⇒ IO[Unit],
    ai: () ⇒ Ai,
    dbRef: User ⇒ DBRef) {

  def ai(config: AiConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withAi config),
      io()
    )
    pov = config.pov
    game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _.withUser(user, dbRef(user))),
      pov.game)
    _ ← gameRepo insert game
    _ ← game.variant.standard.fold(io(), gameRepo saveInitialFen game)
    _ ← timelinePush(game)
    pov2 ← game.player.isHuman.fold(
      io(pov),
      for {
        aiResult ← ai()(game) map (_.err)
        (newChessGame, move) = aiResult
        progress = game.update(newChessGame, move)
        _ ← gameRepo save progress
      } yield pov withGame progress.game
    )
  } yield pov2

  def friend(config: FriendConfig)(implicit ctx: Context): IO[Pov] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withFriend config),
      io()
    )
    pov = config.pov
    game = ctx.me.fold(
      user ⇒ pov.game.updatePlayer(pov.color, _.withUser(user, dbRef(user))),
      pov.game)
    _ ← gameRepo insert game
    _ ← game.variant.standard.fold(io(), gameRepo saveInitialFen game)
    _ ← timelinePush(game)
    _ ← friendConfigMemo.set(pov.game.id, config)
  } yield pov

  def hook(config: HookConfig)(implicit ctx: Context): IO[Hook] = for {
    _ ← ctx.me.fold(
      user ⇒ configRepo.update(user)(_ withHook config),
      io()
    )
    hook = config hook ctx.me
    _ ← fisherman add hook
  } yield hook
}
