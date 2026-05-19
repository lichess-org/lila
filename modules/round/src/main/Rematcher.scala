package lila.round

import chess.format.Fen
import chess.variant.*
import chess.{ Rated, ByColor, Clock, Color as ChessColor, Game as ChessGame, Ply }
import scalalib.cache.ExpireSetMemo

import lila.common.Bus
import lila.core.game.{ GameRepo, IdGenerator }
import lila.core.i18n.{ I18nKey as trans, Translator, defaultLang }
import lila.core.user.{ GameUsers, UserApi }
import lila.game.{ AnonCookie, Event, Rematches, rematchAlternatesColor }

import ChessColor.White

final private class Rematcher(
    gameRepo: GameRepo,
    userApi: UserApi,
    messenger: Messenger,
    onStart: lila.core.game.OnStart,
    rematches: Rematches
)(using Executor, Translator, lila.core.config.RateLimit)(using idGenerator: IdGenerator):

  private given play.api.i18n.Lang = defaultLang

  private val declined = ExpireSetMemo[GameFullId](1.minute)

  private val rateLimit = lila.memo.RateLimit[GameFullId](
    credits = 2,
    duration = 1.minute,
    key = "round.rematch",
    log = false
  )

  private val chess960 = ExpireSetMemo[GameId](3.hours)

  export rematches.isOffering

  def apply(pov: Pov, confirm: Boolean): Fu[Events] =
    if confirm then yes(pov) else no(pov)

  private def couldRematch(g: Game): Boolean =
    g.finishedOrAborted &&
      g.nonMandatory &&
      !g.hasRule(_.noRematch) &&
      !g.boosted &&
      !(g.hasAi && g.variant == FromPosition && g.clock.exists(_.config.limitSeconds < 60))

  def yes(pov: Pov): Fu[Events] =
    pov match
      case Pov(game, color) if couldRematch(game) =>
        if isOffering(!pov.ref) || game.opponent(color).isAi
        then rematches.getAcceptedId(game.id).fold(rematchJoin(pov))(rematchExists(pov))
        else if !declined.get(pov.flip.fullId) && rateLimit.zero(pov.fullId)(true)
        then rematchCreate(pov)
        else fuccess(List(Event.RematchOffer(by = none)))
      case _ => fuccess(List(Event.ReloadOwner))

  def no(pov: Pov): Fu[Events] =
    if isOffering(pov.ref) then
      pov.opponent.userId.foreach: forId =>
        Bus.publishDyn(lila.core.round.RematchCancel(pov.gameId), s"rematchFor:$forId")
      messenger.volatile(pov.game, trans.site.rematchOfferCanceled.txt())
    else if isOffering(!pov.ref) then
      declined.put(pov.fullId)
      messenger.volatile(pov.game, trans.site.rematchOfferDeclined.txt())
    rematches.drop(pov.gameId)
    fuccess(List(Event.RematchOffer(by = none)))

  private def rematchExists(pov: Pov)(nextId: GameId): Fu[Events] =
    gameRepo
      .game(nextId)
      .flatMap:
        _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))

  private def rematchCreate(pov: Pov): Fu[Events] =
    rematches.offer(pov.ref).map { _ =>
      messenger.volatile(pov.game, trans.site.rematchOfferSent.txt())
      pov.opponent.userId.foreach: forId =>
        Bus.publishDyn(lila.core.round.RematchOffer(pov.gameId), s"rematchFor:$forId")
      List(Event.RematchOffer(by = pov.color.some))
    }

  private def rematchJoin(pov: Pov): Fu[Events] =

    def createGame(withId: Option[GameId]) = for
      nextGame <- returnGame(pov, withId).map(_.start)
      _ = rematches.accept(pov.gameId, nextGame.id)
      _ = if pov.game.variant == Chess960 && !chess960.get(pov.gameId) then chess960.put(nextGame.id)
      _ <- gameRepo.insertDenormalized(nextGame)
    yield
      messenger.volatile(pov.game, trans.site.rematchOfferAccepted.txt())
      onStart.exec(nextGame.id)
      incUserColors(nextGame)
      redirectEvents(nextGame)

    rematches.get(pov.gameId) match
      case None => createGame(none)
      case Some(Rematches.NextGame.Accepted(id)) => gameRepo.game(id).mapz(redirectEvents)
      case Some(Rematches.NextGame.Offered(_, id)) => createGame(id.some)

  private def returnGame(pov: Pov, withId: Option[GameId]): Fu[Game] =
    for
      initialFen <- gameRepo.initialFen(pov.game)
      newGame = Rematcher.returnChessGame(
        pov.game.variant,
        pov.game.clock,
        initialFen,
        !chess960.get(pov.gameId)
      )
      users <- userApi.gamePlayersAny(pov.game.userIdPair, pov.game.perfKey)
      sloppy = lila.core.game.newGame(
        chess = newGame,
        players = ByColor(returnPlayer(pov.game, _, users)),
        rated = if users.exists(_.exists(_.user.lame)) then Rated.No else pov.game.rated,
        source = pov.game.source | lila.core.game.Source.Lobby,
        daysPerTurn = pov.game.daysPerTurn,
        pgnImport = None
      )
      game <- withId.fold(idGenerator.withUniqueId(sloppy)): id =>
        fuccess(sloppy.withId(id))
    yield game

  private def incUserColors(game: Game): Unit =
    if game.lobbyOrPool
    then
      game.userIds match
        case List(u1, u2) =>
          userApi.incColor(u1, game.whitePlayer.color)
          userApi.incColor(u2, game.blackPlayer.color)
        case _ => ()

  private def returnPlayer(game: Game, color: ChessColor, users: GameUsers): lila.core.game.Player =
    val fromColor = if rematchAlternatesColor(game, users.mapList(_.map(_.user))) then !color else color
    game.opponent(color).aiLevel match
      case Some(ai) => lila.game.Player.makeAnon(color, ai.some)
      case None => lila.game.Player.make(color, users(fromColor))

  def redirectEvents(game: Game): Events =
    val ownerRedirects = ByColor: color =>
      Event.RedirectOwner(!color, game.fullIdOf(color), AnonCookie.json(game.pov(color)))
    val spectatorRedirect = Event.RematchTaken(game.id)
    spectatorRedirect :: ownerRedirects.toList

object Rematcher:
  // returns a new chess game with the same Board as the previous game
  // except for Chess960, where if shouldRepeatChess960Position is true,
  // the same position is returned otherwise a new random position is returned
  def returnChessGame(
      variant: Variant,
      clock: Option[Clock],
      initialFen: Option[Fen.Full],
      shouldRepeatChess960Position: Boolean
  ): ChessGame =
    val prevPosition = initialFen.flatMap(Fen.readWithMoveNumber(variant, _))
    val newPosition = variant match
      case Chess960 if shouldRepeatChess960Position => prevPosition.fold(Chess960.initialPosition)(_.position)
      case Chess960 => Chess960.initialPosition
      case variant => prevPosition.fold(variant.initialPosition)(_.position)
    val ply = prevPosition.fold(Ply.initial)(_.ply)
    val color = prevPosition.fold[Color](White)(_.position.color)
    ChessGame(
      position = newPosition.withColor(color),
      clock = clock.map(c => Clock(c.config)),
      ply = ply,
      startedAtPly = ply
    )
