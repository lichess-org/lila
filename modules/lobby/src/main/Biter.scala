package lila.lobby

import akka.actor.ActorRef
import chess.{ Game => ChessGame, Board, Variant, Mode, Clock, Color => ChessColor }
import org.joda.time.DateTime

import actorApi.{ RemoveHook, BiteHook, JoinHook }
import lila.game.{ GameRepo, Game, Player, Pov, Progress }
import lila.user.{ User, UserRepo }

private[lobby] final class Biter(blocks: (String, String) => Fu[Boolean]) {

  def apply(hook: Hook, userId: Option[String], uid: String): Fu[JoinHook] =
    userId ?? UserRepo.byId flatMap { user =>
      canJoin(hook, user) flatMap {
        case true  => join(hook, user, uid)
        case false => fufail("%s cannot bite hook %s".format(userId, hook.id))
      }
    }

  private def join(hook: Hook, userOption: Option[User], uid: String): Fu[JoinHook] = for {
    ownerOption ← hook.userId ?? UserRepo.byId
    creatorColor = hook.realColor.resolve
    game = blame(
      !creatorColor, userOption,
      blame(creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← GameRepo insertDenormalized game
  } yield JoinHook(uid, hook, game, creatorColor)

  def blame(color: ChessColor, userOption: Option[User], game: Game) =
    userOption.fold(game)(user => game.updatePlayer(color, _ withUser user))

  private def makeGame(hook: Hook) = Game.make(
    game = ChessGame(
      board = Board init hook.realVariant,
      clock = hook.hasClock.fold(
        hook.time |@| hook.increment apply { (limit, inc) =>
          Clock(limit = limit, increment = inc)
        },
        none)
    ),
    whitePlayer = Player.white,
    blackPlayer = Player.black,
    mode = hook.realMode,
    variant = hook.realVariant,
    source = lila.game.Source.Lobby,
    pgnImport = None)

  def canJoin(hook: Hook, user: Option[User]): Fu[Boolean] =
    if (!hook.open) fuccess(false)
    else hook.realMode.casual.fold(
      user.isDefined || hook.allowAnon,
      user ?? { u =>
        !u.engine && hook.realRatingRange.fold(true)(_ contains u.rating)
      }
    ) ?? !{
        (user |@| hook.userId).tupled ?? {
          case (u, hookUserId) => blocks(hookUserId, u.id) >>| blocks(u.id, hookUserId)
        }
      }
}
