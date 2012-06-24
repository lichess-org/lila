package lila
package ai
package stockfish

import chess.{ Game, Move }
import game.DbGame
import analyse.Analysis

import scalaz.effects._
import dispatch.{ url }

final class Client(
  val playUrl: String,
  analyseUrl: String
) extends ai.Client with Stockfish {

  def play(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = { 
    fetchMove(dbGame.pgn, initialFen | "", dbGame.aiLevel | 1) map { 
      applyMove(dbGame, _)
    }
  }

  def analyse(dbGame: DbGame, initialFen: Option[String]): IO[Valid[Analysis]] = { 
    fetchAnalyse(dbGame.pgn, initialFen | "") map Analysis.decode
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

  private def fetchAnalyse(pgn: String, initialFen: String): IO[String] = io {
    http(analyseUrlObj <<? Map(
      "pgn" -> pgn,
      "initialFen" -> initialFen
    ) as_str)
  }
}
