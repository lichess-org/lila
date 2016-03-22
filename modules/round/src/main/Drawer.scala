package lila.round

import akka.pattern.ask
import chess.{ Game => ChessGame, Board, Clock }

import lila.db.api._
import lila.game.{ Game, Event, Progress, Pov, PlayerRef, Namer, Source }
import lila.pref.{ Pref, PrefApi }
import makeTimeout.short

private[round] final class Drawer(
    messenger: Messenger,
    finisher: Finisher,
    prefApi: PrefApi) {

  def autoThreefold(game: Game)(implicit save: GameProxy.Save): Fu[Option[Pov]] = Pov(game).map { pov =>
    import Pref.PrefZero
    pov.player.userId ?? prefApi.getPref map { pref =>
      pref.autoThreefold == Pref.AutoThreefold.ALWAYS || {
        pref.autoThreefold == Pref.AutoThreefold.TIME &&
          game.clock ?? { _.remainingTime(pov.color) < 30 }
      }
    } map (_ option pov)
  }.sequenceFu map (_.flatten.headOption)

  def yes(pov: Pov)(implicit save: GameProxy.Save): Fu[Events] = pov match {
    case pov if pov.game.toChessHistory.threefoldRepetition =>
      finisher.other(pov.game, _.Draw)
    case pov if pov.opponent.isOfferingDraw =>
      finisher.other(pov.game, _.Draw, None, Some(_.drawOfferAccepted))
    case Pov(g, color) if (g playerCanOfferDraw color) => save {
      messenger.system(g, color.fold(_.whiteOffersDraw, _.blackOffersDraw))
      Progress(g) map { g => g.updatePlayer(color, _ offerDraw g.turns) }
    } inject List(Event.ReloadOwner)
    case _ => fuccess(Nil)
  }

  def no(pov: Pov)(implicit save: GameProxy.Save): Fu[Events] = pov match {
    case Pov(g, color) if pov.player.isOfferingDraw => save {
      messenger.system(g, _.drawOfferCanceled)
      Progress(g) map { g => g.updatePlayer(color, _.removeDrawOffer) }
    } inject List(Event.ReloadOwner)
    case Pov(g, color) if pov.opponent.isOfferingDraw => save {
      messenger.system(g, color.fold(_.whiteDeclinesDraw, _.blackDeclinesDraw))
      Progress(g) map { g => g.updatePlayer(!color, _.removeDrawOffer) }
    } inject List(Event.ReloadOwner)
    case _ => fuccess(Nil)
  }

  def claim(pov: Pov)(implicit save: GameProxy.Save): Fu[Events] =
    (pov.game.playable && pov.game.toChessHistory.threefoldRepetition) ?? finisher.other(pov.game, _.Draw)

  def force(game: Game)(implicit save: GameProxy.Save): Fu[Events] = finisher.other(game, _.Draw, None, None)
}
