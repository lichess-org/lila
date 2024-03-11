package lila.lobby

import shogi.{ Game => ShogiGame, Situation }

import actorApi.{ JoinHook, JoinSeek }
import lila.game.{ Game, PerfPicker, Player }
import lila.socket.Socket.Sri
import lila.user.User

final private class Biter(
    userRepo: lila.user.UserRepo,
    gameRepo: lila.game.GameRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    idGenerator: lila.game.IdGenerator
) {

  def apply(hook: Hook, sri: Sri, user: Option[LobbyUser]): Fu[JoinHook] =
    if (canJoin(hook, user)) join(hook, sri, user)
    else fufail(s"$user cannot bite hook $hook")

  def apply(seek: Seek, user: LobbyUser): Fu[JoinSeek] =
    if (canJoin(seek, user)) join(seek, user)
    else fufail(s"$user cannot join seek $seek")

  private def join(hook: Hook, sri: Sri, lobbyUserOption: Option[LobbyUser]): Fu[JoinHook] =
    for {
      userOption   <- lobbyUserOption.map(_.id) ?? userRepo.byId
      ownerOption  <- hook.userId ?? userRepo.byId
      creatorColor <- assignCreatorColor(ownerOption, userOption, hook.realColor)
      game <- makeGame(
        hook,
        senteUser = creatorColor.fold(ownerOption, userOption),
        goteUser = creatorColor.fold(userOption, ownerOption)
      ).withUniqueId
      _ <- gameRepo insertDenormalized game
    } yield {
      lila.mon.lobby.hook.join.increment()
      JoinHook(sri, hook, game, creatorColor)
    }

  private def join(seek: Seek, lobbyUser: LobbyUser): Fu[JoinSeek] =
    for {
      user         <- userRepo byId lobbyUser.id orFail s"No such user: ${lobbyUser.id}"
      owner        <- userRepo byId seek.user.id orFail s"No such user: ${seek.user.id}"
      creatorColor <- assignCreatorColor(owner.some, user.some, seek.realColor)
      game <- makeGame(
        seek,
        senteUser = creatorColor.fold(owner.some, user.some),
        goteUser = creatorColor.fold(user.some, owner.some)
      ).withUniqueId
      _ <- gameRepo insertDenormalized game
    } yield JoinSeek(user.id, seek, game, creatorColor)

  private def assignCreatorColor(
      creatorUser: Option[User],
      joinerUser: Option[User],
      color: Color
  ): Fu[shogi.Color] =
    color match {
      case Color.Random =>
        userRepo.firstGetsSente(creatorUser.map(_.id), joinerUser.map(_.id)) map shogi.Color.fromSente
      case Color.Sente => fuccess(shogi.Sente)
      case Color.Gote  => fuccess(shogi.Gote)
    }

  private def makeGame(hook: Hook, senteUser: Option[User], goteUser: Option[User]) = {
    val clock      = hook.clock.toClock
    val perfPicker = PerfPicker.mainOrDefault(shogi.Speed(clock.config), hook.realVariant, none)
    Game
      .make(
        shogi = ShogiGame(
          situation = Situation(hook.realVariant),
          clock = clock.some
        ),
        initialSfen = None,
        sentePlayer = Player.make(shogi.Sente, senteUser, perfPicker),
        gotePlayer = Player.make(shogi.Gote, goteUser, perfPicker),
        mode = hook.realMode,
        source = lila.game.Source.Lobby,
        notationImport = None
      )
      .start
  }

  private def makeGame(seek: Seek, senteUser: Option[User], goteUser: Option[User]) = {
    val perfPicker = PerfPicker.mainOrDefault(shogi.Speed(none), seek.realVariant, seek.daysPerTurn)
    Game
      .make(
        shogi = ShogiGame(
          situation = Situation(seek.realVariant),
          clock = none
        ),
        initialSfen = None,
        sentePlayer = Player.make(shogi.Sente, senteUser, perfPicker),
        gotePlayer = Player.make(shogi.Gote, goteUser, perfPicker),
        mode = seek.realMode,
        source = lila.game.Source.Lobby,
        daysPerTurn = seek.daysPerTurn,
        notationImport = None
      )
      .start
  }

  // do not auto join users with anons
  def canAutoJoin(hook: Hook, user: Option[LobbyUser]): Boolean =
    user.isDefined == hook.isAuth && canJoin(hook, user)

  def canJoin(hook: Hook, user: Option[LobbyUser]): Boolean =
    (user.isDefined || !hook.isAuth) && user.fold(true) { u =>
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

  def showHookTo(hook: Hook, member: LobbySocket.Member): Boolean =
    hook.sri == member.sri || canJoin(hook, member.user)
}
