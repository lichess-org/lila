package lila.system

import model._
import lila.chess._
import Pos.posAt
import scalaz.effects._

final class Server(repo: GameRepo, ai: Ai) {

  def playMove(
    fullId: String,
    moveString: String,
    promString: Option[String] = None): IO[Valid[Map[Pos, List[Pos]]]] =
    (decodeMoveString(moveString) toValid "Wrong move").fold(
      e ⇒ io(failure(e)),
      move ⇒ play(fullId, move._1, move._2, promString)
    )

  def play(
    fullId: String,
    fromString: String,
    toString: String,
    promString: Option[String] = None): IO[Valid[Map[Pos, List[Pos]]]] =
    repo playerGame fullId flatMap { game ⇒
      purePlay(game, fromString, toString, promString).fold(
        e ⇒ io(failure(e)),
        a ⇒ repo.applyDiff(game, a) map { _ ⇒ success(a.toChess.situation.destinations) }
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
