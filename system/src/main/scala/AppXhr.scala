package lila.system

import model._
import memo._
import db.GameRepo
import lila.chess._
import Pos.posAt
import scalaz.effects._

case class AppXhr(
    gameRepo: GameRepo,
    ai: Ai,
    finisher: Finisher,
    versionMemo: VersionMemo,
    aliveMemo: AliveMemo) extends IOTools {

  type IOValid = IO[Valid[Unit]]

  def playMove(
    fullId: String,
    moveString: String,
    promString: Option[String] = None): IOValid =
    (decodeMoveString(moveString) toValid "Wrong move").fold(
      e ⇒ io(failure(e)),
      move ⇒ play(fullId, move._1, move._2, promString)
    )

  def play(
    fullId: String,
    fromString: String,
    toString: String,
    promString: Option[String] = None): IOValid = fromPov(fullId) {
    case Pov(g1, color) ⇒ purePlay(g1, fromString, toString, promString).fold(
      e ⇒ io(failure(e)),
      g2 ⇒ for {
        g3 ← if (g2.player(color).isAi) for {
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

  private def purePlay(
    g1: DbGame,
    origString: String,
    destString: String,
    promString: Option[String]): Valid[DbGame] = for {
    g2 ← if (g1.playable) success(g1) else failure("Game is not playable" wrapNel)
    orig ← posAt(origString) toValid "Wrong orig " + origString
    dest ← posAt(destString) toValid "Wrong dest " + destString
    promotion ← Role promotable promString toValid "Wrong promotion " + promString
    chessGame = g2.toChess
    newChessGameAndMove ← chessGame(orig, dest, promotion)
    (newChessGame, move) = newChessGameAndMove
    g3 = g2.update(newChessGame, move)
  } yield g3

  private def decodeMoveString(moveString: String): Option[(String, String)] =
    moveString match {
      case MoveString(orig, dest) ⇒ (orig, dest).some
      case _                      ⇒ none
    }
}
