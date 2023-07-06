package lila.lobby

import chess.{ Game as ChessGame, Situation }

import actorApi.{ JoinHook, JoinSeek }
import lila.game.{ Game, Player }
import lila.socket.Socket.Sri
import lila.user.User

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
      (joinerOption, ownerOption) <- userApi.gamePlayers(
        lobbyUserOption.map(_.id) -> hook.userId,
        hook.perfType
      )
      creatorColor <- assignCreatorColor(ownerOption, joinerOption, hook.realColor)
      game <- makeGame(
        hook,
        whiteUser = creatorColor.fold(ownerOption, joinerOption),
        blackUser = creatorColor.fold(joinerOption, ownerOption)
      ).withUniqueId
      _ <- gameRepo insertDenormalized game
    yield
      lila.mon.lobby.hook.join.increment()
      rememberIfFixedColor(hook.realColor, game)
      JoinHook(sri, hook, game, creatorColor)

  private def join(seek: Seek, lobbyUser: LobbyUser): Fu[JoinSeek] =
    for
      (joiner, owner) <- userApi.gamePlayers.loggedIn(
        lobbyUser.id -> seek.user.id,
        seek.perfType
      ) orFail s"No such seek users: $seek"
      creatorColor <- assignCreatorColor(owner.some, joiner.some, seek.realColor)
      game <- makeGame(
        seek,
        whiteUser = creatorColor.fold(owner.some, joiner.some),
        blackUser = creatorColor.fold(joiner.some, owner.some)
      ).withUniqueId
      _ <- gameRepo insertDenormalized game
    yield
      rememberIfFixedColor(seek.realColor, game)
      JoinSeek(joiner.id, seek, game, creatorColor)

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

  private def makeGame(hook: Hook, whiteUser: Option[User.WithPerf], blackUser: Option[User.WithPerf]) =
    val clock = hook.clock.toClock
    Game
      .make(
        chess = ChessGame(
          situation = Situation(hook.realVariant),
          clock = clock.some
        ),
        whitePlayer = Player.make(chess.White, whiteUser),
        blackPlayer = Player.make(chess.Black, blackUser),
        mode = hook.realMode,
        source = lila.game.Source.Lobby,
        pgnImport = None
      )
      .start

  private def makeGame(seek: Seek, whiteUser: Option[User.WithPerf], blackUser: Option[User.WithPerf]) =
    Game
      .make(
        chess = ChessGame(
          situation = Situation(seek.realVariant),
          clock = none
        ),
        whitePlayer = Player.make(chess.White, whiteUser),
        blackPlayer = Player.make(chess.Black, blackUser),
        mode = seek.realMode,
        source = lila.game.Source.Lobby,
        daysPerTurn = seek.daysPerTurn,
        pgnImport = None
      )
      .start

  def canJoin(hook: Hook, user: Option[LobbyUser]): Boolean =
    hook.isAuth == user.isDefined && user.fold(true): u =>
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
      seek.realRatingRange.fold(true):
        _.contains(user.ratingAt(seek.perfType))

  def showHookTo(hook: Hook, member: LobbySocket.Member): Boolean =
    hook.sri == member.sri || canJoin(hook, member.user)
