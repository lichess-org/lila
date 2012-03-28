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
    promString: Option[String] = None): IOValid =
    gameRepo player fullId flatMap {
      case (g1, player) ⇒ purePlay(g1, fromString, toString, promString).fold(
        e ⇒ io(failure(e)),
        g2 ⇒ for {
          g3 ← if (g2.player.isAi) for {
            aiResult ← ai(g2) map (_.toOption err "AI failure")
            (newChessGame, move) = aiResult
          } yield g2.update(newChessGame, move)
          else io(g2)
          _ ← gameRepo.applyDiff(g1, g3)
          _ ← versionMemo put g3
          _ ← aliveMemo.put(g3.id, player.color)
        } yield success()
      )
    }

  def abort(fullId: String): IOValid = for {
    game ← gameRepo playerGame fullId
    res ← (finisher abort game).sequence
  } yield res

  def resign(fullId: String): IOValid = for {
    gameAndPlayer ← gameRepo player fullId
    (game, player) = gameAndPlayer
    res ← (finisher.resign(game, player.color)).sequence
  } yield res

  def outoftime(fullId: String): IOValid = for {
    game ← gameRepo playerGame fullId
    res ← (finisher outoftime game).sequence
  } yield res

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
