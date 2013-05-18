package lila.round

import chess.{ Game ⇒ ChessGame, Board, Clock, Variant, Color ⇒ ChessColor }
import ChessColor.{ White, Black }
import lila.game.{ GameRepo, Game, Event, Progress, Pov, PlayerRef, Namer, Source }
import lila.user.User
import makeTimeout.short

import lila.game.tube.gameTube
import lila.user.tube.userTube
import lila.db.api._

import akka.pattern.ask

private[round] final class Drawer(
    messenger: Messenger,
    finisher: Finisher) {

  def yes(pov: Pov): Fu[Events] = pov match {
    case pov if pov.opponent.isOfferingDraw ⇒
      finisher(pov.game, _.Draw, None, Some(_.drawOfferAccepted))
    case Pov(g1, color) if (g1 playerCanOfferDraw color) ⇒ for {
      p1 ← messenger.systemMessage(g1, _.drawOfferSent) map { es ⇒
        Progress(g1, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(color, _ offerDraw g.turns) }
      _ ← GameRepo save p2
    } yield p2.events
    case _ ⇒ fufail("[drawer] invalid yes " + pov)
  }

  def no(pov: Pov): Fu[Events] = pov match {
    case Pov(g1, color) if pov.player.isOfferingDraw ⇒ for {
      p1 ← messenger.systemMessage(g1, _.drawOfferCanceled) map { es ⇒
        Progress(g1, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(color, _.removeDrawOffer) }
      _ ← GameRepo save p2
    } yield p2.events
    case Pov(g1, color) if pov.opponent.isOfferingDraw ⇒ for {
      p1 ← messenger.systemMessage(g1, _.drawOfferDeclined) map { es ⇒
        Progress(g1, Event.ReloadTablesOwner :: es)
      }
      p2 = p1 map { g ⇒ g.updatePlayer(!color, _.removeDrawOffer) }
      _ ← GameRepo save p2
    } yield p2.events
    case _ ⇒ fufail("[drawer] invalid no " + pov)
  }

  def claim(pov: Pov): Fu[Events] =
    (pov.game.playable &&
      pov.game.player.color == pov.color &&
      pov.game.toChessHistory.threefoldRepetition
    ) ?? finisher(pov.game, _.Draw)

  def force(game: Game): Fu[Events] = finisher(game, _.Draw, None, None)
}
