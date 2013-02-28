package lila.app
package ai
package stockfish

import chess.{ Game, Move }
import chess.format.UciMove
import game.DbGame
import analyse.Analysis

import scalaz.effects._
import scala.concurrent.Future
import scala.concurrent.duration._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS

final class Client(
    val playUrl: String,
    analyseUrl: String) extends ai.Client with Stockfish {

  def play(dbGame: DbGame, pgn: String, initialFen: Option[String]): Future[Valid[(Game, Move)]] = {
    fetchMove(pgn, initialFen | "", dbGame.aiLevel | 1) map {
      applyMove(dbGame, pgn, _)
    }
  }

  def analyse(pgn: String, initialFen: Option[String]): Future[Valid[Analysis]] =
    fetchAnalyse(pgn, initialFen | "") map {
      Analysis(_, true)
    } recover {
      case e ⇒ !![Analysis](e.getMessage)
    }

  protected def tryPing: Future[Int] = nowMillis |> { start ⇒
    fetchMove(pgn = "", initialFen = "", level = 1) map {
      case move if UciMove(move).isDefined ⇒ (nowMillis - start).toInt
    }
  }

  private def fetchMove(pgn: String, initialFen: String, level: Int): Future[String] =
    WS.url(playUrl).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> initialFen,
      "level" -> level.toString
    ).get() map (_.body)

  private def fetchAnalyse(pgn: String, initialFen: String): Future[String] =
    WS.url(analyseUrl).withQueryString(
      "pgn" -> pgn,
      "initialFen" -> initialFen
    ).get() map (_.body)

  private implicit val executor = Akka.system.dispatcher
}
