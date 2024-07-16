package lila.lobby

import chess.{ ByColor, Game as ChessGame, Situation }

import lila.core.socket.Sri
import lila.core.user.{ GameUsers, WithPerf }

final private class Biter(
    userApi: lila.core.user.UserApi,
    gameRepo: lila.core.game.GameRepo,
    newPlayer: lila.core.game.NewPlayer,
    fixedColor: scalalib.cache.ExpireSetMemo[GameId]
)(using Executor)(using idGenerator: lila.core.game.IdGenerator):

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
      users <- userApi.gamePlayersAny(ByColor(lobbyUserOption.map(_.id), hook.userId), hook.perfType)
      (joiner, owner) = users.toPair
      ownerColor <- assignCreatorColor(owner, joiner, hook.realColor)
      game <- idGenerator.withUniqueId:
        makeGame(
          hook,
          ownerColor.fold(ByColor(owner, joiner), ByColor(joiner, owner))
        )
      _ <- gameRepo.insertDenormalized(game)
    yield
      lila.mon.lobby.hook.join.increment()
      rememberIfFixedColor(hook.realColor, game)
      JoinHook(sri, hook, game, ownerColor)

  private def join(seek: Seek, lobbyUser: LobbyUser): Fu[JoinSeek] =
    for
      users <- userApi
        .gamePlayersLoggedIn(ByColor(lobbyUser.id, seek.user.id), seek.perfType)
        .orFail(s"No such seek users: $seek")
      (joiner, owner) = users.toPair
      ownerColor <- assignCreatorColor(owner.some, joiner.some, seek.realColor)
      game <- idGenerator.withUniqueId:
        makeGame(
          seek,
          ownerColor.fold(ByColor(owner, joiner), ByColor(joiner, owner)).map(some)
        )
      _ <- gameRepo.insertDenormalized(game)
    yield
      rememberIfFixedColor(seek.realColor, game)
      JoinSeek(joiner.id, seek, game, ownerColor)

  private def rememberIfFixedColor(color: TriColor, game: Game) =
    if color != TriColor.Random
    then fixedColor.put(game.id)

  private def assignCreatorColor(
      creatorUser: Option[WithPerf],
      joinerUser: Option[WithPerf],
      color: TriColor
  ): Fu[Color] =
    color match
      case TriColor.Random =>
        userApi.firstGetsWhite(creatorUser.map(_.id), joinerUser.map(_.id)).map { Color.fromWhite(_) }
      case TriColor.White => fuccess(chess.White)
      case TriColor.Black => fuccess(chess.Black)

  private def makeGame(hook: Hook, users: GameUsers) = lila.core.game
    .newGame(
      chess = ChessGame(
        situation = Situation(hook.realVariant),
        clock = hook.clock.toClock.some
      ),
      players = users.mapWithColor(newPlayer.apply),
      mode = hook.realMode,
      source = lila.core.game.Source.Lobby,
      pgnImport = None
    )
    .start

  private def makeGame(seek: Seek, users: GameUsers) = lila.core.game
    .newGame(
      chess = ChessGame(
        situation = Situation(seek.realVariant),
        clock = none
      ),
      players = users.mapWithColor(newPlayer.apply),
      mode = seek.realMode,
      source = lila.core.game.Source.Lobby,
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
