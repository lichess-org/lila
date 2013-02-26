package lila
package importer

import chess.{ Move, Status }
import core.Futuristic
import game.{ DbGame, GameRepo, PovRef }
import round.{ Hand, Finisher }

import scalaz.effects._
import akka.dispatch.Future

final class Importer(
    gameRepo: GameRepo,
    hand: Hand,
    finisher: Finisher,
    bookmark: (String, String) ⇒ IO[Unit]) extends Futuristic {

  val delayInMs = 100

  def apply(data: ImportData, user: Option[String]): Future[Option[DbGame]] =
    gameExists(data.pgn) {
      data preprocess user match {
        case scalaz.Success(Preprocessed(game, moves, result)) ⇒ for {
          _ ← (gameRepo insert game).toFuture
          _ ← (gameRepo denormalize game).toFuture
          _ ← applyMoves(game.id, moves)
          dbGame ← (gameRepo game game.id).toFuture
          _ ← ((result |@| dbGame) apply {
            case (res, dbg) ⇒ finish(dbg, res)
          }) | Future()
          _ ← ((dbGame |@| user) apply {
            case (dbg, u) ⇒ bookmark(dbg.id, u).toFuture
          }) | Future()
        } yield game.some
        case _ ⇒ Future(none)
      }
    }

  private def gameExists(pgn: String)(processing: ⇒ Future[Option[DbGame]]): Future[Option[DbGame]] =
    gameRepo.game(game.Query pgnImport pgn).toFuture flatMap {
      _.fold(game ⇒ Future(game.some), processing)
    }

  private def finish(game: DbGame, result: Result): Future[Unit] = result match {
    case Result(Status.Draw, _)             ⇒ (finisher drawForce game).fold(_ ⇒ io(), _.void).toFuture
    case Result(Status.Resign, Some(color)) ⇒ (hand resign game.fullIdOf(color)).void.toFuture
    case _                                  ⇒ Future()
  }

  private def applyMoves(id: String, moves: List[Move]): Future[Unit] = moves match {
    case Nil ⇒ Future()
    case move :: rest ⇒ applyMove(id, move) flatMap { success ⇒
      success.fold(
        Future(Thread sleep delayInMs) flatMap { _ ⇒ applyMoves(id, rest) },
        Future()
      )
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
