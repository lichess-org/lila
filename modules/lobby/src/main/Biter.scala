package lidraughts.lobby

import draughts.{ DraughtsGame, Situation, Color => DraughtsColor }

import actorApi.{ JoinHook, JoinSeek }
import lidraughts.game.{ GameRepo, Game, Player, PerfPicker }
import lidraughts.user.{ User, UserRepo }

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
    game = makeGame(
      hook,
      whiteUser = creatorColor.fold(ownerOption, userOption),
      blackUser = creatorColor.fold(userOption, ownerOption)
    )
    _ ← GameRepo insertDenormalized game
  } yield {
    lidraughts.mon.lobby.hook.join()
    JoinHook(uid, hook, game, creatorColor)
  }

  private def join(seek: Seek, lobbyUser: LobbyUser): Fu[JoinSeek] = for {
    user ← UserRepo byId lobbyUser.id flatten s"No such user: ${lobbyUser.id}"
    owner ← UserRepo byId seek.user.id flatten s"No such user: ${seek.user.id}"
    creatorColor <- assignCreatorColor(owner.some, user.some, seek.realColor)
    game = makeGame(
      seek,
      whiteUser = creatorColor.fold(owner.some, user.some),
      blackUser = creatorColor.fold(user.some, owner.some)
    )
    _ ← GameRepo insertDenormalized game
  } yield JoinSeek(user.id, seek, game, creatorColor)

  private def assignCreatorColor(
    creatorUser: Option[User],
    joinerUser: Option[User],
    color: Color
  ): Fu[draughts.Color] = color match {
    case Color.Random => UserRepo.firstGetsWhite(creatorUser.map(_.id), joinerUser.map(_.id)) map draughts.Color.apply
    case Color.White => fuccess(draughts.White)
    case Color.Black => fuccess(draughts.Black)
  }

  private def makeGame(hook: Hook, whiteUser: Option[User], blackUser: Option[User]) = {
    val clock = hook.clock.toClock
    val perfPicker = PerfPicker.mainOrDefault(draughts.Speed(clock.config), hook.realVariant, none)
    Game.make(
      draughts = DraughtsGame(
        situation = Situation(hook.realVariant),
        clock = clock.some
      ),
      whitePlayer = Player.make(draughts.White, whiteUser, perfPicker),
      blackPlayer = Player.make(draughts.Black, blackUser, perfPicker),
      mode = hook.realMode,
      source = lidraughts.game.Source.Lobby,
      pdnImport = None
    ).start
  }

  private def makeGame(seek: Seek, whiteUser: Option[User], blackUser: Option[User]) = {
    val perfPicker = PerfPicker.mainOrDefault(draughts.Speed(none), seek.realVariant, seek.daysPerTurn)
    Game.make(
      draughts = DraughtsGame(
        situation = Situation(seek.realVariant),
        clock = none
      ),
      whitePlayer = Player.make(draughts.White, whiteUser, perfPicker),
      blackPlayer = Player.make(draughts.Black, blackUser, perfPicker),
      mode = seek.realMode,
      source = lidraughts.game.Source.Lobby,
      daysPerTurn = seek.daysPerTurn,
      pdnImport = None
    ).start
  }

  def canJoin(hook: Hook, user: Option[LobbyUser]): Boolean =
    (hook.isAuth == user.isDefined || user.isDefined && !hook.isAuth) && user.fold(true) { u =>
      u.lame == hook.lame &&
        !hook.userId.contains(u.id) &&
        !hook.userId.??(u.blocking.contains) &&
        !hook.user.??(_.blocking).contains(u.id) &&
        hook.realRatingRange.fold(true) { range =>
          (hook.perfType map u.ratingAt) ?? range.contains
        }
    }

  def canJoin(seek: Seek, user: LobbyUser): Boolean =
    seek.user.id != user.id &&
      (seek.realMode.casual || user.lame == seek.user.lame) &&
      !(user.blocking contains seek.user.id) &&
      !(seek.user.blocking contains user.id) &&
      seek.realRatingRange.fold(true) { range =>
        (seek.perfType map user.ratingAt) ?? range.contains
      }

  @inline final def showHookTo(hook: Hook, member: actorApi.Member): Boolean =
    hook.uid == member.uid || canJoin(hook, member.user)
}
