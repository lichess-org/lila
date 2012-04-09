package lila

import model._
import memo._
import db.{ GameRepo }
import chess._
import Pos.posAt
import scalaz.effects._

final class AppXhr(
    val gameRepo: GameRepo,
    messenger: Messenger,
    ai: () ⇒ Ai,
    finisher: Finisher,
    aliveMemo: AliveMemo,
    moretimeSeconds: Int) extends IOTools {

  type IOValid = IO[Valid[Unit]]

  def play(
    fullId: String,
    origString: String,
    destString: String,
    promString: Option[String] = None): IOValid = fromPov(fullId) {
    case Pov(g1, color) ⇒ (for {
      g2 ← (g1.playable).fold(success(g1), failure("Game not playable" wrapNel))
      orig ← posAt(origString) toValid "Wrong orig " + origString
      dest ← posAt(destString) toValid "Wrong dest " + destString
      promotion ← Role promotable promString toValid "Wrong promotion"
      newChessGameAndMove ← g2.toChess(orig, dest, promotion)
      (newChessGame, move) = newChessGameAndMove
    } yield g2.update(newChessGame, move)).fold(
      e ⇒ io(failure(e)),
      evented ⇒ for {
        _ ← aliveMemo.put(evented.game.id, color)
        _ ← if (evented.game.finished) for {
          _ ← save(g1, evented)
          _ ← finisher.moveFinish(evented.game, color)
        } yield ()
        else if (evented.game.player.isAi && evented.game.playable) for {
          aiResult ← ai()(evented.game) map (_.err)
          (newChessGame, move) = aiResult
          evented2 = evented flatMap { _.update(newChessGame, move) }
          _ ← save(g1, evented2)
          _ ← finisher.moveFinish(evented2.game, !color)
        } yield ()
        else save(g1, evented)
      } yield success()
    )
  }

  def abort(fullId: String): IOValid = attempt(fullId, finisher.abort)

  def resign(fullId: String): IOValid = attempt(fullId, finisher.resign)

  def forceResign(fullId: String): IOValid = attempt(fullId, finisher.forceResign)

  def outoftime(fullId: String): IOValid = attempt(fullId, finisher outoftime _.game)

  def drawClaim(fullId: String): IOValid = attempt(fullId, finisher.drawClaim)

  def drawAccept(fullId: String): IOValid = attempt(fullId, finisher.drawAccept)

  def drawOffer(fullId: String): IOValid = attempt(fullId, {
    case pov @ Pov(game, color) ⇒
      if (game playerCanOfferDraw color) {
        if (game.player(!color).isOfferingDraw) finisher drawAccept pov
        else success {
          for {
            events ← messenger.systemMessages(game, "Draw offer sent") map { es ⇒
              ReloadTableEvent(!color) :: es
            }
            g2 = game.updatePlayer(color, _ offerDraw game.turns)
            _ ← save(game, Evented(g2, events))
          } yield ()
        }
      }
      else !!("invalid draw offer " + fullId)
  })

  def drawCancel(fullId: String): IOValid = attempt(fullId, {
    case pov @ Pov(game, color) ⇒
      if (pov.player.isOfferingDraw) success {
        for {
          events ← messenger.systemMessages(game, "Draw offer canceled") map { es ⇒
            ReloadTableEvent(!color) :: es
          }
          g2 = game.updatePlayer(color, _.removeDrawOffer)
          _ ← save(game, Evented(g2, events))
        } yield ()
      }
      else !!("no draw offer to cancel " + fullId)
  })

  def drawDecline(fullId: String): IOValid = attempt(fullId, {
    case pov @ Pov(game, color) ⇒
      if (game.player(!color).isOfferingDraw) success {
        for {
          events ← messenger.systemMessages(game, "Draw offer declined") map { es ⇒
            ReloadTableEvent(!color) :: es
          }
          g2 = game.updatePlayer(!color, _.removeDrawOffer)
          _ ← save(game, Evented(g2, events))
        } yield ()
      }
      else !!("no draw offer to decline " + fullId)
  })

  def moretime(fullId: String): IO[Valid[Float]] = attempt(fullId, pov ⇒
    pov.game.clock filter (_ ⇒ pov.game.playable) map { clock ⇒
      val color = !pov.color
      val newClock = clock.giveTime(color, moretimeSeconds)
      val g2 = pov.game withClock newClock
      for {
        events ← messenger.systemMessage(
          g2, "%s + %d seconds".format(color, moretimeSeconds)
        ) map { es ⇒
            MoretimeEvent(color, moretimeSeconds) :: es
          }
        _ ← save(pov.game, Evented(g2, events))
      } yield newClock remainingTime color
    } toValid "cannot add moretime"
  )

  private def attempt[A](
    fullId: String,
    action: Pov ⇒ Valid[IO[A]]): IO[Valid[A]] =
    fromPov(fullId) { pov ⇒ action(pov).sequence }

  private def fromPov[A](fullId: String)(op: Pov ⇒ IO[A]): IO[A] =
    gameRepo pov fullId flatMap op

  private def !!(msg: String) = failure(msg.wrapNel)
}
