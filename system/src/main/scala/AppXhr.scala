package lila.system

import model._
import memo._
import db.{ GameRepo }
import lila.chess._
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

  def claimDraw(fullId: String): IOValid = attempt(fullId, finisher.claimDraw)

  def outoftime(fullId: String): IOValid = attempt(fullId, finisher outoftime _.game)

  def drawAccept(fullId: String): IOValid = attempt(fullId, finisher.drawAccept)

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
}
