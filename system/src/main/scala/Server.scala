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
      move ⇒ play(fullId, move, promString)
    )

  def play(
    fullId: String,
    moveString: (String, String),
    promString: Option[String] = None): IO[Valid[Map[Pos, List[Pos]]]] =
    repo playerGame fullId flatMap { game ⇒
      doPlay(game, fullId, moveString, promString).fold(
        e ⇒ io(failure(e)),
        a ⇒ repo save a map { _ ⇒ success(a.toChess.situation.destinations) }
      )
    }

  def doPlay(
    g1: DbGame,
    fullId: String,
    moveString: (String, String),
    promString: Option[String]): Valid[DbGame] = for {
    g2 ← if (g1.playable) success(g1) else failure("Game is not playable" wrapNel)
    orig ← posAt(moveString._1) toValid "Wrong orig " + moveString
    dest ← posAt(moveString._2) toValid "Wrong dest " + moveString
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
