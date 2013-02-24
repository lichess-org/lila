package lila
package importer

import chess.Move
import core.Futuristic
import game.{ DbGame, GameRepo, PovRef }
import round.Hand

import scalaz.effects._
import akka.dispatch.Future

final class Importer(
    gameRepo: GameRepo,
    hand: Hand) extends Futuristic {

  def apply(data: ImportData): Future[Option[DbGame]] = {
    val (game, moves) = data.game.err
    for {
      _ ← (gameRepo insert game).toFuture
      _ ← (gameRepo denormalize game).toFuture
      success ← applyMoves(game.id, moves)
    } yield success option game
  }

  private def applyMoves(id: String, moves: List[Move]): Future[Boolean] = moves match {
    case Nil ⇒ Future(true)
    case move :: rest ⇒ applyMove(id, move.pp) flatMap { success ⇒
      success.fold(applyMoves(id, rest), Future(false))
    }
  }

  private def applyMove(id: String, move: Move): Future[Boolean] = hand.play(
    povRef = PovRef(id, move.piece.color),
    origString = move.orig.toString,
    destString = move.dest.toString,
    promString = move.promotion map (_.forsyth.toString)
  ) map (_.fold(
      failure ⇒ {
        println("[importer] " + failure.shows)
        false
      },
      _ ⇒ true
    ))
}
