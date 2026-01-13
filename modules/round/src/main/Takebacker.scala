package lila.round

import chess.ByColor
import scalalib.data.Preload

import lila.common.Bus
import lila.core.i18n.{ I18nKey as trans, Translator, defaultLang }
import lila.core.round.*
import lila.game.{ Event, GameRepo, Progress, Rewind, UciMemo }
import lila.pref.{ Pref, PrefApi }
import lila.round.RoundAsyncActor.TakebackBoard
import lila.round.RoundGame.playableByAi
import chess.Ply

final private class Takebacker(
    messenger: Messenger,
    gameRepo: GameRepo,
    uciMemo: UciMemo,
    prefApi: PrefApi
)(using Executor, Translator):

  private given play.api.i18n.Lang = defaultLang

  def apply(board: TakebackBoard)(pov: Pov, confirm: Boolean)(using
      proxy: GameProxy
  ): Fu[(Events, TakebackBoard)] =
    if confirm then yes(board)(pov) else no(board)(pov)

  private def canProposeTakeback(pov: Pov) =
    import pov.game.{ pov as _, * }
    started && playable && !isTournament && !isSimul &&
    bothPlayersHaveMoved &&
    !player(pov.color).isProposingTakeback &&
    !opponent(pov.color).isProposingTakeback

  def yes(board: TakebackBoard)(pov: Pov)(using proxy: GameProxy): Fu[(Events, TakebackBoard)] =
    IfAllowed(pov.game, Preload.none):
      pov match
        case Pov(game, color) if pov.opponent.isProposingTakeback =>
          for
            events <-
              if pov.opponent.proposeTakebackAt == pov.game.ply &&
                color == pov.opponent.proposeTakebackAt.turn
              then single(pov)
              else double(pov)
            _ = publishTakeback(pov)
          yield events -> board.reset
        case Pov(game, _) if pov.game.playableByAi =>
          for
            events <- single(pov)
            _ = publishTakeback(pov)
          yield events -> board
        case Pov(game, _) if pov.opponent.isAi =>
          for
            events <- double(pov)
            _ = publishTakeback(pov)
          yield events -> board
        case pov if canProposeTakeback(pov) && board.offerable =>
          messenger.volatile(pov.game, offerTakebackMessage(pov))
          val progress = Progress(pov.game).map: g =>
            g.updatePlayer(pov.color, _.copy(proposeTakebackAt = g.ply))
          for
            _ <- proxy.save(progress)
            _ = publishTakebackOffer(progress.game)
            events = List(Event.TakebackOffers(pov.color.white, pov.color.black))
          yield events -> board
        case _ => fufail(ClientError("[takebacker] invalid yes " + pov))

  def no(board: TakebackBoard)(pov: Pov)(using proxy: GameProxy): Fu[(Events, TakebackBoard)] =
    pov match
      case Pov(game, color) if pov.player.isProposingTakeback =>
        messenger.volatile(
          game,
          pov.color.fold(trans.site.whiteCancelsTakeback, trans.site.blackCancelsTakeback).txt()
        )
        val progress = Progress(game).map: g =>
          g.updatePlayer(color, _.removeTakebackProposition)
        for
          _ <- proxy.save(progress)
          _ = publishTakebackOffer(progress.game)
          events = List(Event.TakebackOffers(white = false, black = false))
        yield events -> board.decline
      case Pov(game, color) if pov.opponent.isProposingTakeback =>
        messenger.volatile(
          game,
          pov.color.fold(trans.site.whiteDeclinesTakeback, trans.site.blackDeclinesTakeback).txt()
        )
        val progress = Progress(game).map: g =>
          g.updatePlayer(!color, _.removeTakebackProposition)
        for
          _ <- proxy.save(progress)
          _ = publishTakebackOffer(progress.game)
          events = List(Event.TakebackOffers(white = false, black = false))
        yield events -> board.decline
      case _ => fufail(ClientError("[takebacker] invalid no " + pov))

  def isAllowedIn(game: Game, prefs: Preload[ByColor[Pref]]): Fu[Boolean] =
    game.canTakebackOrAddTime.so(isAllowedByPrefs(game, prefs))

  private def offerTakebackMessage(pov: Pov): String =
    val k = if pov.game.turnOf(pov.color) then 2 else 1
    val lastSans = pov.game.sans.takeRight(k).toList
    val startPly = pov.game.ply - k + 1
    def movePrefix(ply: Ply, secondMove: Boolean): String =
      if secondMove && ply.turn.white then ""
      else s"${(ply.value + 1) / 2}${if ply.turn.black then "." else "..."}"
    val rollbackMoves: List[String] =
      lastSans.zipWithIndex.map { case (san, i) =>
        s"${movePrefix(startPly + i, i == 1)}$san"
      }
    val base = pov.color.fold(trans.site.whiteProposesTakeback, trans.site.blackProposesTakeback).txt()
    s"$base (${rollbackMoves.mkString(" ")})"

  private def isAllowedByPrefs(game: Game, prefs: Preload[ByColor[Pref]]): Fu[Boolean] =
    if game.hasAi then fuTrue
    else
      prefs
        .orLoad:
          prefApi.byId(game.userIdPair)
        .dmap:
          _.forall: p =>
            p.takeback == Pref.Takeback.ALWAYS || (p.takeback == Pref.Takeback.CASUAL && game.rated.no)

  private def IfAllowed[A](game: Game, prefs: Preload[ByColor[Pref]])(f: => Fu[A]): Fu[A] =
    if !game.playable then fufail(ClientError("[takebacker] game is over " + game.id))
    else if !game.canTakebackOrAddTime then fufail(ClientError("[takebacker] game disallows it " + game.id))
    else
      isAllowedByPrefs(game, prefs).flatMap:
        if _ then f
        else fufail(ClientError("[takebacker] disallowed by preferences " + game.id))

  private def single(pov: Pov)(using GameProxy): Fu[Events] =
    for
      fen <- gameRepo.initialFen(pov.game)
      progress <- Rewind(pov.game, fen).toFuture
      _ <- fuccess(uciMemo.drop(pov.game, 1))
      events <- saveAndNotify(progress, pov)
    yield events

  private def double(pov: Pov)(using GameProxy): Fu[Events] =
    for
      fen <- gameRepo.initialFen(pov.game)
      prog1 <- Rewind(pov.game, fen).toFuture
      prog2 <- Rewind(prog1.game, fen).toFuture.dmap(progress => prog1.withGame(progress.game))
      _ <- fuccess(uciMemo.drop(pov.game, 2))
      events <- saveAndNotify(prog2, pov)
    yield events

  private def saveAndNotify(p1: Progress, pov: Pov)(using proxy: GameProxy): Fu[Events] =
    val p2 = p1 + Event.Reload
    val accepter = if pov.opponent.isProposingTakeback then pov.color else !pov.color
    messenger.system(
      p2.game,
      accepter.fold(trans.site.whiteAcceptsTakeback, trans.site.blackAcceptsTakeback).txt()
    )
    proxy.save(p2).inject(p2.events)

  private def publishTakebackOffer(game: Game): Unit =
    if game.isCorrespondence && game.nonAi then
      Bus.pub(
        lila.core.round.CorresTakebackOfferEvent(game.id)
      )
    if lila.game.Game.mightBeBoardOrBotCompatible(game) then
      Bus.publishDyn(
        lila.game.actorApi.BoardTakebackOffer(game),
        lila.game.actorApi.BoardTakebackOffer.makeChan(game.id)
      )

  private def publishTakeback(prevPov: Pov)(using proxy: GameProxy): Unit =
    if lila.game.Game.mightBeBoardOrBotCompatible(prevPov.game) then
      proxy.withPov(prevPov.color): p =>
        fuccess:
          Bus.publishDyn(
            lila.game.actorApi.BoardTakeback(p.game),
            lila.game.actorApi.BoardTakeback.makeChan(prevPov.gameId)
          )
