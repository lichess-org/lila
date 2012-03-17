package lila.system

import model._
import memo._
import lila.chess._
import Pos.posAt
import scalaz.effects._

final class Server(repo: GameRepo, ai: Ai, versionMemo: VersionMemo) {

  def playMove(
    fullId: String,
    moveString: String,
    promString: Option[String] = None): IO[Valid[Unit]] =
    (decodeMoveString(moveString) toValid "Wrong move").fold(
      e ⇒ io(failure(e)),
      move ⇒ play(fullId, move._1, move._2, promString)
    )

  def play(
    fullId: String,
    fromString: String,
    toString: String,
    promString: Option[String] = None): IO[Valid[Unit]] =
    repo playerGame fullId flatMap { g1 ⇒
      purePlay(g1, fromString, toString, promString).fold(
        e ⇒ io(failure(e)),
        g2 ⇒ for {
          _ ← repo.applyDiff(g1, g2)
          _ ← versionMemo put g2
        } yield success(Unit)
      )
    }

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
    g4 ← if (g3.player.isAi) aiResponse(g3) else success(g3)
  } yield g4

  private def aiResponse(dbGame: DbGame): Valid[DbGame] = for {
    aiResult ← unsafe { ai(dbGame).unsafePerformIO }
    newChessGameAndMove ← aiResult
    (newChessGame, move) = newChessGameAndMove
  } yield dbGame.update(newChessGame, move)

  private def decodeMoveString(moveString: String): Option[(String, String)] =
    moveString match {
      case MoveString(orig, dest) ⇒ (orig, dest).some
      case _                      ⇒ none
    }
}
