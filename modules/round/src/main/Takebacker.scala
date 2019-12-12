package lila.round

import lila.common.Bus
import lila.game.{ GameRepo, Game, UciMemo, Pov, Rewind, Event, Progress }
import lila.pref.{ Pref, PrefApi }
import RoundDuct.TakebackSituation

private final class Takebacker(
    messenger: Messenger,
    gameRepo: GameRepo,
    uciMemo: UciMemo,
    prefApi: PrefApi
) {

  def yes(situation: TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, TakebackSituation)] = IfAllowed(pov.game) {
    pov match {
      case Pov(game, _) if pov.opponent.isProposingTakeback => {
        if (pov.opponent.proposeTakebackAt == pov.game.turns) single(game)
        else double(game)
      } map (_ -> situation.reset)
      case Pov(game, _) if pov.game.playableByAi => single(game) map (_ -> situation)
      case Pov(game, _) if pov.opponent.isAi => double(game) map (_ -> situation)
      case Pov(game, color) if (game playerCanProposeTakeback color) && situation.offerable => {
        messenger.system(game, _.takebackPropositionSent)
        val progress = Progress(game) map { g =>
          g.updatePlayer(color, _ proposeTakeback g.turns)
        }
        proxy.save(progress) >>- publishTakebackOffer(pov) inject
          List(Event.TakebackOffers(color.white, color.black))
      } map (_ -> situation)
      case _ => fufail(ClientError("[takebacker] invalid yes " + pov))
    }
  }

  def no(situation: TakebackSituation)(pov: Pov)(implicit proxy: GameProxy): Fu[(Events, TakebackSituation)] = pov match {
    case Pov(game, color) if pov.player.isProposingTakeback => proxy.save {
      messenger.system(game, _.takebackPropositionCanceled)
      Progress(game) map { g => g.updatePlayer(color, _.removeTakebackProposition) }
    } inject {
      List(Event.TakebackOffers(false, false)) -> situation.decline
    }
    case Pov(game, color) if pov.opponent.isProposingTakeback => proxy.save {
      messenger.system(game, _.takebackPropositionDeclined)
      Progress(game) map { g => g.updatePlayer(!color, _.removeTakebackProposition) }
    } inject {
      List(Event.TakebackOffers(false, false)) -> situation.decline
    }
    case _ => fufail(ClientError("[takebacker] invalid no " + pov))
  }

  def isAllowedIn(game: Game): Fu[Boolean] =
    if (game.isMandatory) fuFalse
    else isAllowedByPrefs(game)

  private def isAllowedByPrefs(game: Game): Fu[Boolean] =
    if (game.hasAi) fuTrue
    else game.userIds.map {
      prefApi.getPref(_, (p: Pref) => p.takeback)
    }.sequenceFu map {
      _.forall { p =>
        p == Pref.Takeback.ALWAYS || (p == Pref.Takeback.CASUAL && game.casual)
      }
    }

  private def publishTakebackOffer(pov: Pov): Unit =
    if (pov.game.isCorrespondence && pov.game.nonAi && pov.player.hasUser)
      Bus.publish(
        lila.hub.actorApi.round.CorresTakebackOfferEvent(pov.gameId),
        "offerEventCorres"
      )

  private def IfAllowed[A](game: Game)(f: => Fu[A]): Fu[A] =
    if (!game.playable) fufail(ClientError("[takebacker] game is over " + game.id))
    else if (game.isMandatory) fufail(ClientError("[takebacker] game disallows it " + game.id))
    else isAllowedByPrefs(game) flatMap {
      case true => f
      case _ => fufail(ClientError("[takebacker] disallowed by preferences " + game.id))
    }

  private def single(game: Game)(implicit proxy: GameProxy): Fu[Events] = for {
    fen <- gameRepo initialFen game
    progress <- Rewind(game, fen).future
    _ <- fuccess { uciMemo.drop(game, 1) }
    events <- saveAndNotify(progress)
  } yield events

  private def double(game: Game)(implicit proxy: GameProxy): Fu[Events] = for {
    fen <- gameRepo initialFen game
    prog1 <- Rewind(game, fen).future
    prog2 <- Rewind(prog1.game, fen).future map { progress =>
      prog1 withGame progress.game
    }
    _ <- fuccess { uciMemo.drop(game, 2) }
    events <- saveAndNotify(prog2)
  } yield events

  private def saveAndNotify(p1: Progress)(implicit proxy: GameProxy): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, _.takebackPropositionAccepted)
    proxy.save(p2) inject p2.events
  }
}
