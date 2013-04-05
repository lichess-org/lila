package lila.ai
package stockfish

import chess.{ Game, Move }
// import analyse.Analysis

import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._

final class Ai(server: Server) extends lila.ai.Ai {

  import model._

  def play(game: Game, pgn: String, initialFen: Option[String]): Future[Valid[(Game, Move)]] =
    withValidSituation(game) {
      server.play(pgn, initialFen, game.aiLevel | 1) map { validMove ⇒
        validMove flatMap { applyMove(game, pgn, _) }
      }
    }

  // def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]] =
  //   server.analyse(pgn, initialFen)

  private def withValidSituation[A](game: Game)(op: ⇒ Future[Valid[A]]): Future[Valid[A]] =
    if (game.situation playable true) op
    else Future { !!("Invalid game situation: " + game.situation) }

  private implicit val executor = Akka.system.dispatcher

  private def applyMove(game: Game, pgn: String, move: String) = for {
    bestMove ← BestMove(move.some filter ("" !=)).parse toValid "Wrong bestmove " + move
    result ← (game withPgnMoves pgn)(bestMove.orig, bestMove.dest)
  } yield result
}
