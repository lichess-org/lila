package lila.round

import chess.Color
import lila.common.Bus
import lila.game.{ Event, Game, GameRepo, Pov, Progress, Rewind, UciMemo }
import lila.pref.{ Pref, PrefApi }
import lila.i18n.{ I18nKeys => trans, defaultLang }
import RoundAsyncActor.TakebackSituation

final private class Takebacker(
    messenger: Messenger,
    gameRepo: GameRepo,
    uciMemo: UciMemo,
    prefApi: PrefApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  implicit private val chatLang = defaultLang

  def yes(
      situation: TakebackSituation
  )(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    IfAllowed(pov.game) {
      pov match {
        case Pov(game, color) if pov.opponent.isProposingTakeback =>
          {
            if (
              pov.opponent.proposeTakebackAt == pov.game.turns && color == Color
                .fromPly(pov.opponent.proposeTakebackAt)
            ) single(game)
            else double(game)
          } >>- publishTakeback(pov) dmap (_ -> situation.reset)
        case Pov(game, _) if pov.game.playableByAi =>
          single(game) >>- publishTakeback(pov) dmap (_ -> situation)
        case Pov(game, _) if pov.opponent.isAi =>
          double(game) >>- publishTakeback(pov) dmap (_ -> situation)
        case Pov(game, color) if (game playerCanProposeTakeback color) && situation.offerable =>
          {
            messenger.system(game, trans.takebackPropositionSent.txt())
            val newGame = game.updatePlayer(color, _ proposeTakeback game.turns)
            proxy.save(Progress(newGame)) >>-
              publishTakebackOffer(newGame) inject
              List(Event.TakebackOffers(color.white, color.black))
          } dmap (_ -> situation)
        case _ => fufail(ClientError("[takebacker] invalid yes " + pov))
      }
    }

  def no(situation: TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, TakebackSituation)] =
    pov match {
      case Pov(game, color) if pov.player.isProposingTakeback =>
        proxy.save {
          messenger.system(game, trans.takebackPropositionCanceled.txt())
          Progress(game) map { g =>
            g.updatePlayer(color, _.removeTakebackProposition)
          }
        } inject {
          List(Event.TakebackOffers(white = false, black = false)) -> situation.decline
        }
      case Pov(game, color) if pov.opponent.isProposingTakeback =>
        proxy.save {
          messenger.system(game, trans.takebackPropositionDeclined.txt())
          Progress(game) map { g =>
            g.updatePlayer(!color, _.removeTakebackProposition)
          }
        } inject {
          List(Event.TakebackOffers(white = false, black = false)) -> situation.decline
        }
      case _ => fufail(ClientError("[takebacker] invalid no " + pov))
    }

  def isAllowedIn(game: Game): Fu[Boolean] =
    if (game.isMandatory) fuFalse
    else isAllowedByPrefs(game)

  private def isAllowedByPrefs(game: Game): Fu[Boolean] =
    if (game.hasAi) fuTrue
    else
      game.userIds.map {
        prefApi.getPref(_, (p: Pref) => p.takeback)
      }.sequenceFu dmap {
        _.forall { p =>
          p == Pref.Takeback.ALWAYS || (p == Pref.Takeback.CASUAL && game.casual)
        }
      }

  private def IfAllowed[A](game: Game)(f: => Fu[A]): Fu[A] =
    if (!game.playable) fufail(ClientError("[takebacker] game is over " + game.id))
    else if (game.isMandatory) fufail(ClientError("[takebacker] game disallows it " + game.id))
    else
      isAllowedByPrefs(game) flatMap {
        case true => f
        case _    => fufail(ClientError("[takebacker] disallowed by preferences " + game.id))
      }

  private def single(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    for {
      fen      <- gameRepo initialFen game
      progress <- Rewind(game, fen).toFuture
      _        <- fuccess { uciMemo.drop(game, 1) }
      events   <- saveAndNotify(progress)
    } yield events

  private def double(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    for {
      fen   <- gameRepo initialFen game
      prog1 <- Rewind(game, fen).toFuture
      prog2 <- Rewind(prog1.game, fen).toFuture dmap { progress =>
        prog1 withGame progress.game
      }
      _      <- fuccess { uciMemo.drop(game, 2) }
      events <- saveAndNotify(prog2)
    } yield events

  private def saveAndNotify(p1: Progress)(implicit proxy: GameProxy): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, trans.takebackPropositionAccepted.txt())
    proxy.save(p2) inject p2.events
  }

  private def publishTakebackOffer(game: Game): Unit =
    if (lila.game.Game.isBoardCompatible(game))
      Bus.publish(
        lila.game.actorApi.BoardDrawOffer(game),
        lila.game.actorApi.BoardDrawOffer makeChan game.id
      )

  private def publishTakeback(prevPov: Pov)(implicit proxy: GameProxy): Unit =
    if (lila.game.Game.isBoardCompatible(prevPov.game))
      proxy
        .withPov(prevPov.color) { p =>
          fuccess(
            Bus.publish(
              lila.game.actorApi.BoardTakeback(p.game),
              lila.game.actorApi.BoardTakeback makeChan prevPov.gameId
            )
          )
        }
        .unit

}
