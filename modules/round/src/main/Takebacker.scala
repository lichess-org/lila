package lila.round

import chess.{ ByColor, Color }

import lila.common.Bus
import scalalib.data.Preload
import lila.core.i18n.{ I18nKey as trans, Translator, defaultLang }
import lila.core.round.*
import lila.game.{ Event, GameRepo, Progress, Rewind, UciMemo }
import lila.pref.{ Pref, PrefApi }
import lila.round.RoundAsyncActor.TakebackSituation
import lila.round.RoundGame.playableByAi

final private class Takebacker(
    messenger: Messenger,
    gameRepo: GameRepo,
    uciMemo: UciMemo,
    prefApi: PrefApi
)(using Executor, Translator):

  private given play.api.i18n.Lang = defaultLang

  def apply(
      situation: TakebackSituation
  )(pov: Pov, confirm: Boolean)(using proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    if confirm then yes(situation)(pov) else no(situation)(pov)

  private def canProposeTakeback(pov: Pov) =
    import pov.game.{ pov as _, * }
    started && playable && !isTournament && !isSimul &&
    bothPlayersHaveMoved &&
    !player(pov.color).isProposingTakeback &&
    !opponent(pov.color).isProposingTakeback

  def yes(
      situation: TakebackSituation
  )(pov: Pov)(using proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    IfAllowed(pov.game, Preload.none):
      pov match
        case Pov(game, color) if pov.opponent.isProposingTakeback =>
          for
            events <-
              if pov.opponent.proposeTakebackAt == pov.game.ply &&
                color == pov.opponent.proposeTakebackAt.turn
              then single(game)
              else double(game)
            _ = publishTakeback(pov)
          yield events -> situation.reset
        case Pov(game, _) if pov.game.playableByAi =>
          for
            events <- single(game)
            _ = publishTakeback(pov)
          yield events -> situation
        case Pov(game, _) if pov.opponent.isAi =>
          for
            events <- double(game)
            _ = publishTakeback(pov)
          yield events -> situation
        case pov if canProposeTakeback(pov) && situation.offerable =>
          messenger.volatile(pov.game, trans.site.takebackPropositionSent.txt())
          val progress = Progress(pov.game).map: g =>
            g.updatePlayer(pov.color, _.copy(proposeTakebackAt = g.ply))
          for
            _ <- proxy.save(progress)
            _      = publishTakebackOffer(progress.game)
            events = List(Event.TakebackOffers(pov.color.white, pov.color.black))
          yield events -> situation
        case _ => fufail(ClientError("[takebacker] invalid yes " + pov))

  def no(situation: TakebackSituation)(pov: Pov)(using proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    pov match
      case Pov(game, color) if pov.player.isProposingTakeback =>
        messenger.volatile(game, trans.site.takebackPropositionCanceled.txt())
        val progress = Progress(game).map: g =>
          g.updatePlayer(color, _.removeTakebackProposition)
        for
          _ <- proxy.save(progress)
          _      = publishTakebackOffer(progress.game)
          events = List(Event.TakebackOffers(white = false, black = false))
        yield events -> situation.decline
      case Pov(game, color) if pov.opponent.isProposingTakeback =>
        messenger.volatile(game, trans.site.takebackPropositionDeclined.txt())
        val progress = Progress(game).map: g =>
          g.updatePlayer(!color, _.removeTakebackProposition)
        for
          _ <- proxy.save(progress)
          _      = publishTakebackOffer(progress.game)
          events = List(Event.TakebackOffers(white = false, black = false))
        yield events -> situation.decline
      case _ => fufail(ClientError("[takebacker] invalid no " + pov))

  def isAllowedIn(game: Game, prefs: Preload[ByColor[Pref]]): Fu[Boolean] =
    game.canTakebackOrAddTime.so(isAllowedByPrefs(game, prefs))

  private def isAllowedByPrefs(game: Game, prefs: Preload[ByColor[Pref]]): Fu[Boolean] =
    if game.hasAi then fuTrue
    else
      prefs
        .orLoad:
          prefApi.byId(game.userIdPair)
        .dmap:
          _.forall: p =>
            p.takeback == Pref.Takeback.ALWAYS || (p.takeback == Pref.Takeback.CASUAL && game.casual)

  private def IfAllowed[A](game: Game, prefs: Preload[ByColor[Pref]])(f: => Fu[A]): Fu[A] =
    if !game.playable then fufail(ClientError("[takebacker] game is over " + game.id))
    else if !game.canTakebackOrAddTime then fufail(ClientError("[takebacker] game disallows it " + game.id))
    else
      isAllowedByPrefs(game, prefs).flatMap:
        if _ then f
        else fufail(ClientError("[takebacker] disallowed by preferences " + game.id))

  private def single(game: Game)(using GameProxy): Fu[Events] =
    for
      fen      <- gameRepo.initialFen(game)
      progress <- Rewind(game, fen).toFuture
      _        <- fuccess(uciMemo.drop(game, 1))
      events   <- saveAndNotify(progress)
    yield events

  private def double(game: Game)(using GameProxy): Fu[Events] =
    for
      fen    <- gameRepo.initialFen(game)
      prog1  <- Rewind(game, fen).toFuture
      prog2  <- Rewind(prog1.game, fen).toFuture.dmap(progress => prog1.withGame(progress.game))
      _      <- fuccess(uciMemo.drop(game, 2))
      events <- saveAndNotify(prog2)
    yield events

  private def saveAndNotify(p1: Progress)(using proxy: GameProxy): Fu[Events] =
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, trans.site.takebackPropositionAccepted.txt())
    proxy.save(p2).inject(p2.events)

  private def publishTakebackOffer(game: Game): Unit =
    if lila.game.Game.mightBeBoardOrBotCompatible(game) then
      Bus.publish(
        lila.game.actorApi.BoardTakebackOffer(game),
        lila.game.actorApi.BoardTakebackOffer.makeChan(game.id)
      )

  private def publishTakeback(prevPov: Pov)(using proxy: GameProxy): Unit =
    if lila.game.Game.mightBeBoardOrBotCompatible(prevPov.game) then
      proxy.withPov(prevPov.color): p =>
        fuccess:
          Bus.publish(
            lila.game.actorApi.BoardTakeback(p.game),
            lila.game.actorApi.BoardTakeback.makeChan(prevPov.gameId)
          )
