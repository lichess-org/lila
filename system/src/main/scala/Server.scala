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
    (game, player) = gameAndPlayer
    chessGame = game.toChess
    newChessGameAndMove ← chessGame(orig, dest, promotion)
    (newChessGame, move) = newChessGameAndMove
    g1 = game update newChessGame
    eventStacks = game.eventStacks mapValues (_ withMove move)
    g2 = g1 withEventStacks eventStacks
    result ← unsafe { repo save g2 }
  } yield newChessGame.situation.destinations

  private def moveToEvents(move: Move): Map[DbPlayer, EventStack] = Map.empty

  private def decodeMoveString(moveString: String): Option[(String, String)] = moveString match {
    case MoveString(orig, dest) ⇒ (orig, dest).some
    case _                      ⇒ none
  }
}
