package lila.round

import akka.pattern.ask
import chess.{ Game => ChessGame, Board, Clock, Variant }

import lila.db.api._
import lila.game.tube.gameTube
import lila.game.{ GameRepo, Game, Event, Progress, Pov, PlayerRef, Namer, Source }
import lila.user.tube.userTube
import lila.user.User
import makeTimeout.short

private[round] final class Drawer(messenger: Messenger, finisher: Finisher) {

  def yes(pov: Pov): Fu[Events] = pov match {
    case pov if pov.opponent.isOfferingDraw =>
      finisher(pov.game, _.Draw, None, Some(_.drawOfferAccepted))
    case Pov(g, color) if (g playerCanOfferDraw color) => GameRepo save {
      messenger.system(g, _.drawOfferSent)
      Progress(g) map { g => g.updatePlayer(color, _ offerDraw g.turns) }
    } inject List(Event.ReloadTablesOwner)
    case _ => fufail("[drawer] invalid yes " + pov)
  }

  def no(pov: Pov): Fu[Events] = pov match {
    case Pov(g, color) if pov.player.isOfferingDraw => GameRepo save {
      messenger.system(g, _.drawOfferCanceled)
      Progress(g) map { g => g.updatePlayer(color, _.removeDrawOffer) }
    } inject List(Event.ReloadTablesOwner)
    case Pov(g, color) if pov.opponent.isOfferingDraw => GameRepo save {
      messenger.system(g, _.drawOfferDeclined)
      Progress(g) map { g => g.updatePlayer(!color, _.removeDrawOffer) }
    } inject List(Event.ReloadTablesOwner)
    case _ => fufail("[drawer] invalid no " + pov)
  }

  def claim(pov: Pov): Fu[Events] =
    (pov.game.playable && pov.game.toChessHistory.threefoldRepetition) ?? finisher(pov.game, _.Draw)

  def force(game: Game): Fu[Events] = finisher(game, _.Draw, None, None)
}
