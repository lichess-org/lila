package lila.lobby

import akka.actor.ActorRef
import chess.{ Game => ChessGame, Board, Mode, Clock, Color => ChessColor }
import org.joda.time.DateTime

import actorApi.{ RemoveHook, BiteHook, BiteSeek, JoinHook, JoinSeek, LobbyUser }
import lila.game.{ GameRepo, Game, Player, Pov, Progress, PerfPicker }
import lila.user.{ User, UserRepo }

private[lobby] object Biter {

  def apply(hook: Hook, uid: String, user: Option[LobbyUser]): Fu[JoinHook] =
    if (canJoin(hook, user)) join(hook, uid, user)
    else fufail(s"$user cannot bite hook $hook")

  def apply(seek: Seek, user: LobbyUser): Fu[JoinSeek] =
    if (canJoin(seek, user)) join(seek, user)
    else fufail(s"$user cannot join seek $seek")

  private def join(hook: Hook, uid: String, lobbyUserOption: Option[LobbyUser]): Fu[JoinHook] = for {
    userOption ← lobbyUserOption.map(_.id) ?? UserRepo.byId
    ownerOption ← hook.userId ?? UserRepo.byId
    creatorColor <- assignCreatorColor(ownerOption, userOption, hook.realColor)
    game = blame(
      !creatorColor, userOption,
      blame(creatorColor, ownerOption, makeGame(hook))
    ).start
    _ ← GameRepo insertDenormalized game
  } yield JoinHook(uid, hook, game, creatorColor)

  private def join(seek: Seek, lobbyUser: LobbyUser): Fu[JoinSeek] = for {
    user ← UserRepo byId lobbyUser.id flatten s"No such user: ${lobbyUser.id}"
    owner ← UserRepo byId seek.user.id flatten s"No such user: ${seek.user.id}"
    creatorColor <- assignCreatorColor(owner.some, user.some, seek.realColor)
    game = blame(
      !creatorColor, user.some,
      blame(creatorColor, owner.some, makeGame(seek))
    ).start
    _ ← GameRepo insertDenormalized game
  } yield JoinSeek(user.id, seek, game, creatorColor)

  private def assignCreatorColor(
    creatorUser: Option[User],
    joinerUser: Option[User],
    color: Color): Fu[chess.Color] = color match {
    case Color.Random => UserRepo.firstGetsWhite(creatorUser.map(_.id), joinerUser.map(_.id)) map chess.Color.apply
    case Color.White  => fuccess(chess.White)
    case Color.Black  => fuccess(chess.Black)
  }

  private def blame(color: ChessColor, userOption: Option[User], game: Game) =
    userOption.fold(game) { user =>
      game.updatePlayer(color, _.withUser(user.id, PerfPicker.mainOrDefault(game)(user.perfs)))
    }

  private def makeGame(hook: Hook) = Game.make(
    game = ChessGame(
      board = Board init hook.realVariant,
      clock = hook.clock.some),
    whitePlayer = Player.white,
    blackPlayer = Player.black,
    mode = hook.realMode,
    variant = hook.realVariant,
    source = lila.game.Source.Lobby,
    pgnImport = None)

  private def makeGame(seek: Seek) = Game.make(
    game = ChessGame(
      board = Board init seek.realVariant,
      clock = none),
    whitePlayer = Player.white,
    blackPlayer = Player.black,
    mode = seek.realMode,
    variant = seek.realVariant,
    source = lila.game.Source.Lobby,
    daysPerTurn = seek.daysPerTurn,
    pgnImport = None)

  def canJoin(hook: Hook, user: Option[LobbyUser]): Boolean =
    hook.realMode.casual.fold(
      user.isDefined || hook.allowAnon,
      user ?? { _.lame == hook.lame }
    ) &&
      !(user ?? (u => hook.userId contains u.id)) &&
      !(hook.userId ?? (user ?? (_.blocking)).contains) &&
      !(user.map(_.id) ?? (hook.user ?? (_.blocking)).contains) &&
      hook.realRatingRange.fold(true) { range =>
        user ?? { u =>
          (hook.perfType map (_.key) flatMap u.ratingMap.get) ?? range.contains
        }
      }

  def canJoin(seek: Seek, user: LobbyUser): Boolean =
    seek.user.id != user.id &&
      (seek.realMode.casual || user.lame == seek.user.lame) &&
      !(user.blocking contains seek.user.id) &&
      !(seek.user.blocking contains user.id) &&
      seek.realRatingRange.fold(true) { range =>
        (seek.perfType map (_.key) flatMap user.ratingMap.get) ?? range.contains
      }
}
