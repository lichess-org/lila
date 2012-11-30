package lila
package setup

import lobby.{ HookRepo, Hook, Fisherman }
import user.{ User, UserRepo }
import chess.{ Game, Board, Variant, Mode, Clock, Color ⇒ ChessColor }
import game.{ GameRepo, DbGame, DbPlayer, Pov }
import round.{ Messenger, Progress }

import scalaz.effects._

final class HookJoiner(
    hookRepo: HookRepo,
    fisherman: Fisherman,
    gameRepo: GameRepo,
    userRepo: UserRepo,
    timelinePush: DbGame ⇒ IO[Unit],
    messenger: Messenger) {

  def apply(hookId: String, myHookId: Option[String])(me: Option[User]): IO[Valid[Pov]] =
    for {
      hookOption ← hookRepo hook hookId
      myHookOption ← ~(myHookId map hookRepo.ownedHook)
      result ← hookOption.fold(io(!![Pov]("No such hook"))) { hook ⇒
        if (canJoin(hook, me)) join(hook, myHookOption)(me) map success
        else io(!![Pov]("Can not join hook"))
      }
    } yield result

  private def join(hook: Hook, myHook: Option[Hook])(me: Option[User]): IO[Pov] = for {
    _ ← ~(myHook map fisherman.delete)
    ownerOption ← hook.userId.fold(io(none[User]))(userRepo.byId)
    game = blame(
      _.invitedColor, me,
      blame(_.creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← gameRepo insert game
    _ ← gameRepo denormalizeStarted game
    _ ← timelinePush(game)
    // messenges are not sent to the game socket
    // as nobody is there to see them yet
    _ ← messenger init game
    _ ← fisherman.bite(hook, game)
  } yield Pov(game, game.invitedColor)

  def blame(color: DbGame ⇒ ChessColor, userOption: Option[User], game: DbGame) =
    userOption.fold(game)(user ⇒ game.updatePlayer(color(game), _ withUser user))

  def makeGame(hook: Hook) = DbGame(
    game = Game(
      board = Board init hook.realVariant,
      clock = hook.hasClock.fold(
        hook.time |@| hook.increment apply { (limit, inc) ⇒
          Clock(limit = limit, increment = inc)
        },
        none)
    ),
    ai = None,
    whitePlayer = DbPlayer.white,
    blackPlayer = DbPlayer.black,
    creatorColor = hook.realColor.resolve,
    mode = hook.realMode,
    variant = hook.realVariant)

  private def canJoin(hook: Hook, me: Option[User]) =
    !hook.`match` && {
      hook.realMode.casual || (me exists { u ⇒
        hook.realEloRange.fold(true)(_ contains u.elo)
      })
    }
}
