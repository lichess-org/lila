package lila.setup

import lila.lobby.{ HookRepo, Hook, Fisherman }
import lila.user.{ User, UserRepo }
import chess.{ Game ⇒ ChessGame, Board, Variant, Mode, Clock, Color ⇒ ChessColor }
import lila.game.{ GameRepo, Game, Player, Pov, Progress }
import lila.round.Messenger

import lila.user.tube.userTube
import lila.lobby.tube.hookTube
import lila.game.tube.gameTube
import lila.db.api._

import akka.actor.ActorRef

private[setup] final class HookJoiner(
    fisherman: Fisherman,
    timeline: ActorRef,
    messenger: Messenger) {

  def apply(hookId: String, myHookId: Option[String])(me: Option[User]): Fu[Valid[Pov]] = for {
    hookOption ← $find.byId[Hook](hookId)
    myHookOption ← myHookId zmap HookRepo.ownedHook
    result ← hookOption.fold(fuccess(!![Pov]("No such hook"))) { hook ⇒
      if (canJoin(hook, me)) join(hook, myHookOption)(me) map success
      else fuccess(!![Pov]("Can not join hook"))
    }
  } yield result

  private def join(hook: Hook, myHook: Option[Hook])(me: Option[User]): Fu[Pov] = for {
    _ ← myHook zmap fisherman.delete
    ownerOption ← hook.userId zmap $find.byId[User]
    game = blame(
      _.invitedColor, me,
      blame(_.creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← $insert(game) >>
      (GameRepo denormalize game) >>-
      (timeline ! game) >>
      // messenges are not sent to the game socket
      // as nobody is there to see them yet
      (messenger init game) >>
      fisherman.bite(hook, game)
  } yield Pov(game, game.invitedColor)

  def blame(color: Game ⇒ ChessColor, userOption: Option[User], game: Game) =
    userOption.fold(game)(user ⇒ game.updatePlayer(color(game), _ withUser user))

  private def makeGame(hook: Hook) = Game.make(
    game = ChessGame(
      board = Board init hook.realVariant,
      clock = hook.hasClock.fold(
        hook.time |@| hook.increment apply { (limit, inc) ⇒
          Clock(limit = limit, increment = inc)
        },
        none)
    ),
    ai = None,
    whitePlayer = Player.white,
    blackPlayer = Player.black,
    creatorColor = hook.realColor.resolve,
    mode = hook.realMode,
    variant = hook.realVariant,
    source = lila.game.Source.Lobby,
    pgnImport = None)

  private def canJoin(hook: Hook, me: Option[User]) = !hook.`match` && {
    hook.realMode.casual || (me exists { u ⇒
      hook.realEloRange.fold(true)(_ contains u.elo)
    })
  }
}
