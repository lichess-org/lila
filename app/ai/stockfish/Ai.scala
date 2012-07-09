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

  def play(dbGame: DbGame, initialFen: Option[String]): Future[Valid[(Game, Move)]] =
    server.play(dbGame.pgn, initialFen, dbGame.aiLevel | 1) map { validMove ⇒
      validMove flatMap { applyMove(dbGame, _) }
    }

  def analyse(dbGame: DbGame, initialFen: Option[String]): Future[Valid[Analysis]] =
    server.analyse(dbGame.pgn, initialFen)

  private implicit val executor = Akka.system.dispatcher
}
