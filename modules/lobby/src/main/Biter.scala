package lila.lobby

import akka.actor.ActorRef
import chess.{ Game => ChessGame, Board, Variant, Mode, Clock, Color => ChessColor }
import org.joda.time.DateTime

import actorApi.{ RemoveHook, BiteHook, JoinHook, LobbyUser }
import lila.game.{ GameRepo, Game, Player, Pov, Progress, PerfPicker }
import lila.user.{ User, UserRepo }

private[lobby] object Biter {

  def apply(hook: Hook, uid: String, user: Option[LobbyUser]): Fu[JoinHook] =
    canJoin(hook, user).fold(
      join(hook, uid, user),
      fufail(s"$user cannot bite hook $hook")
    )

  private def join(hook: Hook, uid: String, lobbyUserOption: Option[LobbyUser]): Fu[JoinHook] = for {
    userOption ← lobbyUserOption.map(_.id) ?? UserRepo.byId
    ownerOption ← hook.userId ?? UserRepo.byId
    creatorColor = hook.realColor.resolve
    game = blame(
      !creatorColor, userOption,
      blame(creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← GameRepo insertDenormalized game
  } yield JoinHook(uid, hook, game, creatorColor)

  private def blame(color: ChessColor, userOption: Option[User], game: Game) =
    userOption.fold(game) { user =>
      game.updatePlayer(color, _.withUser(user.id, PerfPicker.mainOrDefault(game)(user.perfs)))
    }

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
    daysPerTurn = hook.daysPerTurn,
    pgnImport = None)

  def canJoin(hook: Hook, user: Option[LobbyUser]): Boolean = hook.open &&
    hook.realMode.casual.fold(
      user.isDefined || hook.allowAnon,
      user ?? { _.engine == hook.engine }
    ) &&
      !(hook.userId ?? (user ?? (_.blocking)).contains) &&
      !((user map (_.id)) ?? (hook.user ?? (_.blocking)).contains) &&
      hook.realRatingRange.fold(true) { range =>
        user ?? { u =>
          (hook.perfType map (_.key) flatMap u.ratingMap.get) ?? range.contains
        }
      }
}
