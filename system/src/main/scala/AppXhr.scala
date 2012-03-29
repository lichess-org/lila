package lila.system

import model._
import memo._
import db.GameRepo
import lila.chess._
import Pos.posAt
import scalaz.effects._

final class AppXhr(
    gameRepo: GameRepo,
    ai: Ai,
    finisher: Finisher,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo) {

  type IOValid = IO[Valid[Unit]]

  def playMove(
    fullId: String,
    moveString: String,
    promString: Option[String] = None): IOValid = moveString match {
    case MoveString(orig, dest) ⇒ play(fullId, orig, dest, promString)
    case _                      ⇒ io(failure("Wrong move" wrapNel))
  }

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
      g2 ⇒ for {
        g3 ← if (g2.player.isAi) for {
          aiResult ← ai(g2) map (_.toOption err "AI failure")
          (newChessGame, move) = aiResult
        } yield g2.update(newChessGame, move)
        else io(g2)
        _ ← gameRepo.applyDiff(g1, g3)
        _ ← versionMemo put g3
        _ ← aliveMemo.put(g3.id, color)
      } yield success()
    )
  }

  def abort(fullId: String): IOValid = attempt(fullId, finisher.abort)

  def resign(fullId: String): IOValid = attempt(fullId, finisher.resign)

  def forceResign(fullId: String): IOValid = attempt(fullId, finisher.forceResign)

  def claimDraw(fullId: String): IOValid = attempt(fullId, finisher.claimDraw)

  def outoftime(fullId: String): IOValid = attempt(fullId, finisher.outoftime)

  private def attempt(fullId: String, action: Pov ⇒ Valid[IO[Unit]]): IOValid =
    fromPov(fullId) { pov ⇒ action(pov).sequence }

  private def fromPov[A](fullId: String)(op: Pov ⇒ IO[A]): IO[A] =
    gameRepo pov fullId flatMap op
}
