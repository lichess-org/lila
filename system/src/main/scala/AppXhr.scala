package lila.system

import model._
import memo._
import db.{ GameRepo, RoomRepo }
import lila.chess._
import Pos.posAt
import scalaz.effects._

final class AppXhr(
    val gameRepo: GameRepo,
    roomRepo: RoomRepo,
    ai: Ai,
    finisher: Finisher,
    val versionMemo: VersionMemo,
    aliveMemo: AliveMemo) extends IOTools {

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

  def drawAccept(fullId: String): IOValid = attempt(fullId, finisher.drawAccept)

  def talk(fullId: String, message: String): IOValid = attempt(fullId, pov ⇒
    if (pov.game.invited.isHuman && message.size <= 140 && message.nonEmpty)
      success(for {
      _ ← roomRepo.addMessage(pov.game.id, pov.color.name, message)
      g2 = pov.game withEvents List(MessageEvent(pov.color.name, message))
      _ ← save(pov.game, g2)
    } yield ())
    else failure("Cannot talk" wrapNel)
  )

  private def attempt(fullId: String, action: Pov ⇒ Valid[IO[Unit]]): IOValid =
    fromPov(fullId) { pov ⇒ action(pov).sequence }

  private def fromPov[A](fullId: String)(op: Pov ⇒ IO[A]): IO[A] =
    gameRepo pov fullId flatMap op
}
