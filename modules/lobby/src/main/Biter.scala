package lila.lobby

import actorApi.{ RemoveHook, BiteHook, JoinHook }
import lila.user.{ User, UserRepo }
import chess.{ Game ⇒ ChessGame, Board, Variant, Mode, Clock, Color ⇒ ChessColor }
import lila.game.{ GameRepo, Game, Player, Pov, Progress }
import lila.lobby.tube.hookTube
import lila.db.api._

import akka.actor.ActorRef

private[lobby] final class Biter(
    timeline: lila.hub.ActorLazyRef,
    roundMessenger: lila.round.Messenger) {

  def apply(hookId: String, userId: Option[String]): Fu[String ⇒ JoinHook] = for {
    hookOption ← $find.byId[Hook](hookId)
    userOption ← userId ?? UserRepo.byId
    result ← hookOption.fold[Fu[String ⇒ JoinHook]](fufail("No such hook")) { hook ⇒
      if (canJoin(hook, userOption)) join(hook, userOption)
      else fufail("Can not join hook")
    }
  } yield result

  private def join(hook: Hook, userOption: Option[User]): Fu[String ⇒ JoinHook] = for {
    ownerOption ← hook.userId ?? UserRepo.byId
    game = blame(
      _.invitedColor, userOption,
      blame(_.creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← (GameRepo insertDenormalized game) >>-
      (timeline ! game) >>
      // messenges are not sent to the game socket
      // as nobody is there to see them yet
      (roundMessenger init game)
  } yield uid ⇒ JoinHook(uid, hook, game)

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

  private def canJoin(hook: Hook, userOption: Option[User]) = !hook.`match` && {
    hook.realMode.casual || (userOption exists { u ⇒
      hook.realEloRange.fold(true)(_ contains u.elo)
    })
  }
}
