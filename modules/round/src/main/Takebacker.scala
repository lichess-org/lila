package lila.round

import chess.Color
import lila.common.Bus
import lila.game.{ Event, Game, GameRepo, Pov, Progress, Rewind, UciMemo }
import lila.pref.{ Pref, PrefApi }
import lila.i18n.{ defaultLang, I18nKeys as trans }
import RoundAsyncActor.TakebackSituation

final private class Takebacker(
    messenger: Messenger,
    gameRepo: GameRepo,
    uciMemo: UciMemo,
    prefApi: PrefApi
)(using Executor):

  private given play.api.i18n.Lang = defaultLang

  def yes(
      situation: TakebackSituation
  )(pov: Pov)(using proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    IfAllowed(pov.game) {
      pov match
        case Pov(game, color) if pov.opponent.isProposingTakeback =>
          {
            if pov.opponent.proposeTakebackAt == pov.game.ply &&
              color == pov.opponent.proposeTakebackAt.turn
            then single(game)
            else double(game)
          } andDo publishTakeback(pov) dmap (_ -> situation.reset)
        case Pov(game, _) if pov.game.playableByAi =>
          single(game) andDo publishTakeback(pov) dmap (_ -> situation)
        case Pov(game, _) if pov.opponent.isAi =>
          double(game) andDo publishTakeback(pov) dmap (_ -> situation)
        case Pov(game, color) if (game playerCanProposeTakeback color) && situation.offerable =>
          {
            messenger.volatile(game, trans.takebackPropositionSent.txt())
            val progress = Progress(game) map { g =>
              g.updatePlayer(color, _ proposeTakeback g.ply)
            }
            proxy.save(progress) andDo
              publishTakebackOffer(progress.game) inject
              List(Event.TakebackOffers(color.white, color.black))
          } dmap (_ -> situation)
        case _ => fufail(ClientError("[takebacker] invalid yes " + pov))
    }

  def no(situation: TakebackSituation)(pov: Pov)(using proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    pov match
      case Pov(game, color) if pov.player.isProposingTakeback =>
        messenger.volatile(game, trans.takebackPropositionCanceled.txt())
        val progress = Progress(game) map { g =>
          g.updatePlayer(color, _.removeTakebackProposition)
        }
        proxy.save(progress) andDo
          publishTakebackOffer(progress.game) inject
          List(Event.TakebackOffers(white = false, black = false)) -> situation.decline
      case Pov(game, color) if pov.opponent.isProposingTakeback =>
        messenger.volatile(game, trans.takebackPropositionDeclined.txt())
        val progress = Progress(game) map { g =>
          g.updatePlayer(!color, _.removeTakebackProposition)
        }
        proxy.save(progress) andDo
          publishTakebackOffer(progress.game) inject
          List(Event.TakebackOffers(white = false, black = false)) -> situation.decline
      case _ => fufail(ClientError("[takebacker] invalid no " + pov))

  def isAllowedIn(game: Game): Fu[Boolean] =
    game.canTakebackOrAddTime so isAllowedByPrefs(game)

  private def isAllowedByPrefs(game: Game): Fu[Boolean] =
    if game.hasAi then fuTrue
    else
      game.userIds.map {
        prefApi.get(_, (p: Pref) => p.takeback)
      }.parallel dmap {
        _.forall { p =>
          p == Pref.Takeback.ALWAYS || (p == Pref.Takeback.CASUAL && game.casual)
        }
      }

  private def IfAllowed[A](game: Game)(f: => Fu[A]): Fu[A] =
    if !game.playable then fufail(ClientError("[takebacker] game is over " + game.id))
    else if !game.canTakebackOrAddTime then fufail(ClientError("[takebacker] game disallows it " + game.id))
    else
      isAllowedByPrefs(game) flatMap {
        if _ then f
        else fufail(ClientError("[takebacker] disallowed by preferences " + game.id))
      }

  private def single(game: Game)(using GameProxy): Fu[Events] =
    for
      fen      <- gameRepo initialFen game
      progress <- Rewind(game, fen).toFuture
      _        <- fuccess { uciMemo.drop(game, 1) }
      events   <- saveAndNotify(progress)
    yield events

  private def double(game: Game)(using GameProxy): Fu[Events] =
    for
      fen   <- gameRepo initialFen game
      prog1 <- Rewind(game, fen).toFuture
      prog2 <- Rewind(prog1.game, fen).toFuture dmap { progress =>
        prog1 withGame progress.game
      }
      _      <- fuccess { uciMemo.drop(game, 2) }
      events <- saveAndNotify(prog2)
    yield events

  private def saveAndNotify(p1: Progress)(using proxy: GameProxy): Fu[Events] =
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, trans.takebackPropositionAccepted.txt())
    proxy.save(p2) inject p2.events

  private def publishTakebackOffer(game: Game): Unit =
    if lila.game.Game.isBoardOrBotCompatible(game) then
      Bus.publish(
        lila.game.actorApi.BoardTakebackOffer(game),
        lila.game.actorApi.BoardTakebackOffer makeChan game.id
      )

  private def publishTakeback(prevPov: Pov)(using proxy: GameProxy): Unit =
    if lila.game.Game.isBoardOrBotCompatible(prevPov.game) then
      proxy.withPov(prevPov.color): p =>
        fuccess:
          Bus.publish(
            lila.game.actorApi.BoardTakeback(p.game),
            lila.game.actorApi.BoardTakeback makeChan prevPov.gameId
          )
