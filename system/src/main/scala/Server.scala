package lila.system

import model._
import lila.chess._
import Pos.posAt

final class Server(repo: GameRepo) {

  def playMove(
    fullId: String,
    moveString: String,
    promString: Option[String] = None): Valid[Map[Pos, List[Pos]]] = for {
    moveParts ← decodeMoveString(moveString) toValid "Wrong move"
    (origString, destString) = moveParts
    orig ← posAt(origString) toValid "Wrong orig " + origString
    dest ← posAt(destString) toValid "Wrong dest " + destString
    promotion ← Role promotable promString toValid "Wrong promotion " + promString
    gameAndPlayer ← repo player fullId toValid "Wrong ID " + fullId
    (g1, _) = gameAndPlayer
    g2 ← if (g1.playable) success(g1) else failure("Game is not playable" wrapNel)
    chessGame = g2.toChess
    newChessGameAndMove ← chessGame(orig, dest, promotion)
    (newChessGame, move) = newChessGameAndMove
    g3 = g2.update(newChessGame, move)
    g4 ← if (g3.player.isAi) aiResponse(g3) else success(g3)
    result ← unsafe { repo save g3 }
  } yield newChessGame.situation.destinations

  private def aiResponse(dbGame: DbGame): Valid[DbGame] = {
    success(dbGame)
  }

  private def moveToEvents(move: Move): Map[DbPlayer, EventStack] = Map.empty

  private def decodeMoveString(moveString: String): Option[(String, String)] = moveString match {
    case MoveString(orig, dest) ⇒ (orig, dest).some
    case _                      ⇒ none
  }
}
