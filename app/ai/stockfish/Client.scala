package lila
package ai
package stockfish

import chess.{ Game, Move }
import chess.format.UciMove
import game.DbGame
import analyse.Analysis

import scalaz.effects._
import dispatch.{ url }
import akka.dispatch.{ Future, Await }
import akka.util.duration._
import play.api.Play.current
import play.api.libs.concurrent._

final class Client(
    val playUrl: String,
    analyseUrl: String) extends ai.Client with Stockfish {

  def play(dbGame: DbGame, initialFen: Option[String]): Future[Valid[(Game, Move)]] = {
    fetchMove(dbGame.pgn, initialFen | "", dbGame.aiLevel | 1) map {
      applyMove(dbGame, _)
    }
  }

  def analyse(dbGame: DbGame, initialFen: Option[String]): Future[Valid[Analysis]] =
    fetchAnalyse(dbGame.pgn, initialFen | "") map {
      _ flatMap { Analysis(_, true) }
    }

  private lazy val analyseUrlObj = url(analyseUrl)

  protected lazy val tryPing: Option[Int] = nowMillis |> { start ⇒
    (unsafe {
      Await.result(fetchMove(pgn = "", initialFen = "", level = 1), 5 seconds)
    }).toOption flatMap {
      case move if UciMove(move).isDefined ⇒ Some(nowMillis - start) map (_.toInt)
      case _                               ⇒ none[Int]
    }
  }

  private def fetchMove(pgn: String, initialFen: String, level: Int): Future[String] = Future {
    http(playUrlObj <<? Map(
      "pgn" -> pgn,
      "initialFen" -> initialFen,
      "level" -> level.toString
    ) as_str)
  }

  private def fetchAnalyse(pgn: String, initialFen: String): Future[Valid[String]] = Future {
    unsafe {
      http(analyseUrlObj <<? Map(
        "pgn" -> pgn,
        "initialFen" -> initialFen
      ) as_str)
    }
  }

  private implicit val executor = Akka.system.dispatcher
}
