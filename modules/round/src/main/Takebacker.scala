package lila.round

import shogi.Color
import lila.common.Bus
import lila.game.{ Event, Game, Pov, Progress, Rewind }
import lila.pref.{ Pref, PrefApi }
import lila.i18n.{ defaultLang, I18nKeys => trans }
import RoundDuct.TakebackSituation

final private class Takebacker(
    messenger: Messenger,
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
              pov.opponent.proposeTakebackAt == pov.game.plies && color == Color
                .fromPly(pov.opponent.proposeTakebackAt)
            ) single(game)
            else double(game)
          } dmap (_ -> situation.reset)
        case Pov(game, _) if pov.game.playableByAi => single(game) dmap (_ -> situation)
        case Pov(game, _) if pov.opponent.isAi     => double(game) dmap (_ -> situation)
        case Pov(game, color) if (game playerCanProposeTakeback color) && situation.offerable =>
          {
            messenger.system(game, trans.takebackPropositionSent.txt())
            val progress = Progress(game) map { g =>
              g.updatePlayer(color, _ proposeTakeback g.plies)
            }
            proxy.save(progress) >>- publishTakebackOffer(pov) inject
              List(Event.TakebackOffers(color.sente, color.gote))
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
          List(Event.TakebackOffers(false, false)) -> situation.decline
        }
      case Pov(game, color) if pov.opponent.isProposingTakeback =>
        proxy.save {
          messenger.system(game, trans.takebackPropositionDeclined.txt())
          Progress(game) map { g =>
            g.updatePlayer(!color, _.removeTakebackProposition)
          }
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
    else
      game.userIds.map {
        prefApi.getPref(_, (p: Pref) => p.takeback)
      }.sequenceFu dmap {
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
    if (!game.playable) fufail(ClientError("[takebacker] game is over or paused" + game.id))
    else if (game.isMandatory) fufail(ClientError("[takebacker] game disallows it " + game.id))
    else
      isAllowedByPrefs(game) flatMap {
        case true => f
        case _    => fufail(ClientError("[takebacker] disallowed by preferences " + game.id))
      }

  private def single(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    for {
      progress <- Rewind(game).toFuture
      events   <- saveAndNotify(progress)
    } yield events

  private def double(game: Game)(implicit proxy: GameProxy): Fu[Events] =
    for {
      prog1 <- Rewind(game).toFuture
      prog2 <- Rewind(prog1.game).toFuture dmap { progress =>
        prog1 withGame progress.game
      }
      events <- saveAndNotify(prog2)
    } yield events

  private def saveAndNotify(p1: Progress)(implicit proxy: GameProxy): Fu[Events] = {
    val p2 = p1 + Event.Reload
    messenger.system(p2.game, trans.takebackPropositionAccepted.txt())
    proxy.save(p2) inject p2.events
  }
}
