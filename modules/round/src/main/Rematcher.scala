package lila.round

import chess.format.Fen
import chess.variant.*
import chess.{ Board, Castles, Clock, ByColor, Color as ChessColor, Ply, Game as ChessGame, Situation }
import ChessColor.{ Black, White }

import lila.common.Bus
import lila.game.{ AnonCookie, Event, Game, GameRepo, Pov, Rematches, Source }
import lila.memo.ExpireSetMemo
import lila.user.{ User, UserApi, GameUsers }
import lila.i18n.{ defaultLang, I18nKeys as trans }

final private class Rematcher(
    gameRepo: GameRepo,
    userApi: UserApi,
    messenger: Messenger,
    onStart: OnStart,
    rematches: Rematches
)(using Executor, lila.game.IdGenerator):

  private given play.api.i18n.Lang = defaultLang

  private val declined = ExpireSetMemo[GameFullId](1 minute)

  private val rateLimit = lila.memo.RateLimit[GameFullId](
    credits = 2,
    duration = 1 minute,
    key = "round.rematch",
    log = false
  )

  private val chess960 = ExpireSetMemo[GameId](3 hours)

  def isOffering(pov: Pov): Boolean = rematches isOffering pov.ref

  def yes(pov: Pov): Fu[Events] =
    pov match
      case Pov(game, color) if game.playerCouldRematch =>
        if isOffering(!pov) || game.opponent(color).isAi
        then rematches.getAcceptedId(game.id).fold(rematchJoin(pov))(rematchExists(pov))
        else if !declined.get(pov.flip.fullId) && rateLimit.zero(pov.fullId)(true)
        then rematchCreate(pov)
        else fuccess(List(Event.RematchOffer(by = none)))
      case _ => fuccess(List(Event.ReloadOwner))

  def no(pov: Pov): Fu[Events] =
    if isOffering(pov) then
      pov.opponent.userId.foreach: forId =>
        Bus.publish(lila.hub.actorApi.round.RematchCancel(pov.gameId), s"rematchFor:$forId")
      messenger.volatile(pov.game, trans.rematchOfferCanceled.txt())
    else if isOffering(!pov) then
      declined put pov.fullId
      messenger.volatile(pov.game, trans.rematchOfferDeclined.txt())
    rematches.drop(pov.gameId)
    fuccess(List(Event.RematchOffer(by = none)))

  private def rematchExists(pov: Pov)(nextId: GameId): Fu[Events] =
    gameRepo game nextId flatMap:
      _.fold(rematchJoin(pov))(g => fuccess(redirectEvents(g)))

  private def rematchCreate(pov: Pov): Fu[Events] =
    rematches.offer(pov.ref) map { _ =>
      messenger.volatile(pov.game, trans.rematchOfferSent.txt())
      pov.opponent.userId.foreach: forId =>
        Bus.publish(lila.hub.actorApi.round.RematchOffer(pov.gameId), s"rematchFor:$forId")
      List(Event.RematchOffer(by = pov.color.some))
    }

  private def rematchJoin(pov: Pov): Fu[Events] =

    def createGame(withId: Option[GameId]) = for
      nextGame <- returnGame(pov, withId).map(_.start)
      _ = rematches.accept(pov.gameId, nextGame.id)
      _ = if pov.game.variant == Chess960 && !chess960.get(pov.gameId) then chess960.put(nextGame.id)
      _ <- gameRepo insertDenormalized nextGame
    yield
      messenger.volatile(pov.game, trans.rematchOfferAccepted.txt())
      onStart(nextGame.id)
      redirectEvents(nextGame)

    rematches.get(pov.gameId) match
      case None                                    => createGame(none)
      case Some(Rematches.NextGame.Accepted(id))   => gameRepo game id mapz redirectEvents
      case Some(Rematches.NextGame.Offered(_, id)) => createGame(id.some)

  private def returnGame(pov: Pov, withId: Option[GameId]): Fu[Game] =
    for
      initialFen <- gameRepo initialFen pov.game
      newGame = Rematcher.returnChessGame(
        pov.game.variant,
        pov.game.clock,
        initialFen,
        !chess960.get(pov.gameId)
      )
      users <- userApi.gamePlayers(pov.game.userIdPair, pov.game.perfType)
      sloppy = Game.make(
        chess = newGame,
        players = ByColor(returnPlayer(pov.game, _, users)),
        mode = if users.exists(_.exists(_.user.lame)) then chess.Mode.Casual else pov.game.mode,
        source = pov.game.source | Source.Lobby,
        daysPerTurn = pov.game.daysPerTurn,
        pgnImport = None
      )
      game <- withId.fold(sloppy.withUniqueId) { id => fuccess(sloppy withId id) }
    yield game

  private def returnPlayer(game: Game, color: ChessColor, users: GameUsers): lila.game.Player =
    game.opponent(color).aiLevel match
      case Some(ai) => lila.game.Player.makeAnon(color, ai.some)
      case None     => lila.game.Player.make(color, users(!color))

  private def redirectEvents(game: Game): Events =
    val ownerRedirects = ByColor: color =>
      Event.RedirectOwner(color, game fullIdOf color, AnonCookie.json(game pov color))
    val spectatorRedirect = Event.RematchTaken(game.id)
    spectatorRedirect :: ownerRedirects.toList

object Rematcher:
  // returns a new chess game with the same Situation as the previous game
  // except for Chess960, where if shouldRepeatChess960Position is true,
  // the same position is returned otherwise a new random position is returned
  def returnChessGame(
      variant: Variant,
      clock: Option[Clock],
      initialFen: Option[Fen.Epd],
      shouldRepeatChess960Position: Boolean
  ): ChessGame =
    val prevSituation = initialFen.flatMap(Fen.readWithMoveNumber(variant, _))
    val newSituation = variant match
      case Chess960 if shouldRepeatChess960Position => prevSituation.fold(Situation(Chess960))(_.situation)
      case Chess960                                 => Situation(Chess960)
      case variant                                  => prevSituation.fold(Situation(variant))(_.situation)
    val ply   = prevSituation.fold(Ply.initial)(_.ply)
    val color = prevSituation.fold[chess.Color](White)(_.situation.color)
    ChessGame(
      situation = newSituation.copy(color = color),
      clock = clock.map(c => Clock(c.config)),
      ply = ply,
      startedAtPly = ply
    )
