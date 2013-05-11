package lila.importer

import chess.{ Move, Status }
import lila.game.{ Game, GameRepo, PovRef }
import lila.round.{ Hand, Finisher }

import lila.game.tube.gameTube
import lila.db.api._

import akka.actor.ActorRef
import scala.concurrent.duration.Duration

private[importer] final class Importer(
    hand: Hand,
    finisher: Finisher,
    bookmark: ActorRef,
    delay: Duration) {

  def apply(data: ImportData, user: Option[String]): Fu[Game] = gameExists(data.pgn) {
    (data preprocess user).fold[Fu[Game]](fufail(_), {
        case Preprocessed(game, moves, result) ⇒ for {
          _ ← (GameRepo insertDenormalized game) >> applyMoves(game.id, moves)
          dbGame ← $find.byId[Game](game.id)
          _ ← ~((dbGame |@| result) apply {
            case (dbg, res) ⇒ finish(dbg, res)
          }) >>- ~((dbGame |@| user) apply {
            case (dbg, u) ⇒ bookmark ! (dbg.id -> u)
          })
        } yield game
      })
  }

  private def gameExists(pgn: String)(processing: ⇒ Fu[Game]): Fu[Game] =
    $find.one(lila.game.Query pgnImport pgn) flatMap {
      _.fold(processing)(game ⇒ fuccess(game))
    }

  private def finish(game: Game, result: Result): Funit = result match {
    case Result(Status.Draw, _)             ⇒ (finisher drawForce game).void
    case Result(Status.Resign, Some(color)) ⇒ (hand resign game.fullIdOf(!color)).void
    case _                                  ⇒ funit
  }

  private def applyMoves(id: String, moves: List[Move]): Funit = moves match {
    case Nil ⇒ funit
    case move :: rest ⇒ applyMove(id, move) flatMap {
      _ ?? { applyMoves(id, rest) >>- (Thread sleep delay.toMillis) }
    }
  }

  private def applyMove(id: String, move: Move): Fu[Boolean] = hand.play(
    povRef = PovRef(id, move.piece.color),
    origString = move.orig.toString,
    destString = move.dest.toString,
    promString = move.promotion map (_.forsyth.toString)
  ) inject true recover {
      case e ⇒ {
        logwarn("[importer] " + e.getMessage)
        false
      }
    }
}
