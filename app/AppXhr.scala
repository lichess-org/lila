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
    ai: Ai,
    finisher: Finisher,
    val versionMemo: VersionMemo,
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
      g3 ⇒ for {
        _ ← aliveMemo.put(g3.id, color)
        _ ← if (g3.finished) for {
          _ ← save(g1, g3)
          _ ← finisher.moveFinish(g3, color)
        } yield ()
        else if (g3.player.isAi && g3.playable) for {
          aiResult ← ai(g3) map (_.err)
          (newChessGame, move) = aiResult
          g4 = g3.update(newChessGame, move)
          _ ← save(g1, g4)
          _ ← finisher.moveFinish(g4, !color)
        } yield ()
        else save(g1, g3)
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
            g2 ← messenger.systemMessages(game, "Draw offer sent")
            g3 = g2.updatePlayer(color, (_ offerDraw game.turns))
            g4 = g3.withEvents(!color, List(ReloadTableEvent()))
            _ ← save(game, g4)
          } yield ()
        }
      }
      else !!("invalid draw offer " + fullId)
  })

  def drawCancel(fullId: String): IOValid = attempt(fullId, {
    case pov @ Pov(game, color) ⇒
      if (pov.player.isOfferingDraw) success {
        for {
          g2 ← messenger.systemMessages(game, "Draw offer canceled")
          g3 = g2.updatePlayer(color, (_.copy(isOfferingDraw = false)))
          g4 = g3.withEvents(!color, List(ReloadTableEvent()))
          _ ← save(game, g4)
        } yield ()
      }
      else !!("no draw offer to cancel " + fullId)
  })

  def drawDecline(fullId: String): IOValid = attempt(fullId, {
    case pov @ Pov(game, color) ⇒
      if (game.player(!color).isOfferingDraw) success {
        for {
          g2 ← messenger.systemMessages(game, "Draw offer declined")
          g3 = g2.updatePlayer(!color, (_.copy(isOfferingDraw = false)))
          g4 = g3.withEvents(!color, List(ReloadTableEvent()))
          _ ← save(game, g4)
        } yield ()
      }
      else !!("no draw offer to decline " + fullId)
  })

  def talk(fullId: String, message: String): IO[Unit] = fromPov(fullId) { pov ⇒
    messenger.playerMessage(pov.game, pov.color, message) flatMap { g2 ⇒
      save(pov.game, g2)
    }
  }

  def moretime(fullId: String): IO[Valid[Float]] = attempt(fullId, pov ⇒
    pov.game.clock filter (_ ⇒ pov.game.playable) map { clock ⇒
      val color = !pov.color
      val newClock = clock.giveTime(color, moretimeSeconds)
      val g2 = pov.game withEvents List(MoretimeEvent(color, moretimeSeconds))
      val g3 = g2 withClock newClock
      for {
        g4 ← messenger.systemMessage(
          g3,
          "%s + %d seconds".format(color, moretimeSeconds))
        _ ← save(pov.game, g4)
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
