package lila
package ai
package stockfish

import chess.{ Game, Move }
import game.DbGame
import analyse.Analysis

import scalaz.effects._
import akka.dispatch.Future
import play.api.Play.current
import play.api.libs.concurrent._

final class Ai(server: Server) extends lila.ai.Ai with Stockfish {

  import model._

  def play(dbGame: DbGame, pgn: String, initialFen: Option[String]): Future[Valid[(Game, Move)]] =
    withValidSituation(dbGame) {
      server.play(pgn, initialFen, dbGame.aiLevel | 1) map { validMove ⇒
        validMove flatMap { applyMove(dbGame, pgn, _) }
      }
    }

  def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]] =
    server.analyse(pgn, initialFen)

  private def withValidSituation[A](dbGame: DbGame)(op: ⇒ Future[Valid[A]]): Future[Valid[A]] =
    if (dbGame.toChess.situation.playable) op
    else Future { !!("Invalid game situation: " + dbGame.toChess.situation) }

  private implicit val executor = Akka.system.dispatcher
}
