package lila
package ai
package stockfish

import chess.{ Game, Move }
import game.DbGame
import analyse.Analysis

import scalaz.effects._
import dispatch.{ url }
import akka.dispatch.Future
import play.api.Play.current
import play.api.libs.concurrent._

final class Client(
    val playUrl: String,
    analyseUrl: String) extends ai.Client with Stockfish {

  def play(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = {
    fetchMove(dbGame.pgn, initialFen | "", dbGame.aiLevel | 1) map {
      applyMove(dbGame, _)
    }
  }

  def analyse(dbGame: DbGame, initialFen: Option[String]): Future[Valid[Analysis]] =
    fetchAnalyse(dbGame.pgn, initialFen | "") map { 
      _ flatMap { Analysis(_, true) }
    } 

  private lazy val analyseUrlObj = url(analyseUrl)

  protected def tryPing: IO[Option[Int]] = for {
    start ← io(nowMillis)
    received ← fetchMove(
      pgn = "",
      initialFen = "",
      level = 1
    ).catchLeft map (_.isRight)
    delay ← io(nowMillis - start)
  } yield received option delay.toInt

  private def fetchMove(pgn: String, initialFen: String, level: Int): IO[String] = io {
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
