package lila.lobby

import chess.{ Game as ChessGame, Situation, ByColor }

import actorApi.{ JoinHook, JoinSeek }
import lila.game.{ Game, Player }
import lila.socket.Socket.Sri
import lila.user.{ User, GameUsers }

final private class Biter(
    userRepo: lila.user.UserRepo,
    userApi: lila.user.UserApi,
    gameRepo: lila.game.GameRepo
)(using Executor, lila.game.IdGenerator):

  def apply(hook: Hook, sri: Sri, user: Option[LobbyUser]): Fu[JoinHook] =
    if canJoin(hook, user)
    then join(hook, sri, user)
    else fufail(s"$user cannot bite hook $hook")

  def apply(seek: Seek, user: LobbyUser): Fu[JoinSeek] =
    if canJoin(seek, user)
    then join(seek, user)
    else fufail(s"$user cannot join seek $seek")

  private def join(hook: Hook, sri: Sri, lobbyUserOption: Option[LobbyUser]): Fu[JoinHook] =
    for
      users <- userApi.gamePlayers(ByColor(lobbyUserOption.map(_.id), hook.userId), hook.perfType)
      (joiner, owner) = users.toPair
      ownerColor <- assignCreatorColor(owner, joiner, hook.realColor)
      game <- makeGame(
        hook,
        ownerColor.fold(ByColor(owner, joiner), ByColor(joiner, owner))
      ).withUniqueId
      _ <- gameRepo insertDenormalized game
    yield
      lila.mon.lobby.hook.join.increment()
      rememberIfFixedColor(hook.realColor, game)
      JoinHook(sri, hook, game, ownerColor)

  private def join(seek: Seek, lobbyUser: LobbyUser): Fu[JoinSeek] =
    for
      users <- userApi.gamePlayers.loggedIn(
        ByColor(lobbyUser.id, seek.user.id),
        seek.perfType
      ) orFail s"No such seek users: $seek"
      (joiner, owner) = users.toPair
      ownerColor <- assignCreatorColor(owner.some, joiner.some, seek.realColor)
      game <- makeGame(
        seek,
        ownerColor.fold(ByColor(owner, joiner), ByColor(joiner, owner)).map(some)
      ).withUniqueId
      _ <- gameRepo insertDenormalized game
    yield
      rememberIfFixedColor(seek.realColor, game)
      JoinSeek(joiner.id, seek, game, ownerColor)

  private def rememberIfFixedColor(color: Color, game: Game) =
    if color != Color.Random
    then gameRepo.fixedColorLobbyCache put game.id

  private def assignCreatorColor(
      creatorUser: Option[User.WithPerf],
      joinerUser: Option[User.WithPerf],
      color: Color
  ): Fu[chess.Color] =
    color match
      case Color.Random =>
        userRepo.firstGetsWhite(creatorUser.map(_.id), joinerUser.map(_.id)) map { chess.Color.fromWhite(_) }
      case Color.White => fuccess(chess.White)
      case Color.Black => fuccess(chess.Black)

  private def makeGame(hook: Hook, users: GameUsers) = Game
    .make(
      chess = ChessGame(
        situation = Situation(hook.realVariant),
        clock = hook.clock.toClock.some
      ),
      players = users.mapWithColor(Player.make),
      mode = hook.realMode,
      source = lila.game.Source.Lobby,
      pgnImport = None
    )
    .start

  private def makeGame(seek: Seek, users: GameUsers) = Game
    .make(
      chess = ChessGame(
        situation = Situation(seek.realVariant),
        clock = none
      ),
      players = users.mapWithColor(Player.make),
      mode = seek.realMode,
      source = lila.game.Source.Lobby,
      daysPerTurn = seek.daysPerTurn,
      pgnImport = None
    )
    .start

  def canJoin(hook: Hook, user: Option[LobbyUser]): Boolean =
    hook.isAuth == user.isDefined && user.forall: u =>
      u.lame == hook.lame &&
        !hook.userId.contains(u.id) &&
        !hook.userId.so(u.blocking.value.contains) &&
        !hook.user.so(_.blocking).value.contains(u.id) &&
        hook.ratingRangeOrDefault.contains(u.ratingAt(hook.perfType))

  def canJoin(seek: Seek, user: LobbyUser): Boolean =
    seek.user.id != user.id &&
      (seek.realMode.casual || user.lame == seek.user.lame) &&
      !(user.blocking.value contains seek.user.id) &&
      !(seek.user.blocking.value contains user.id) &&
      seek.realRatingRange.forall:
        _.contains(user.ratingAt(seek.perfType))

  def showHookTo(hook: Hook, member: LobbySocket.Member): Boolean =
    hook.sri == member.sri || canJoin(hook, member.user)
