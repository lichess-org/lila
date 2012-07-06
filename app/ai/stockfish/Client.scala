package lila
package ai
package stockfish

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._

final class Client(val remoteUrl: String) extends ai.Client with Stockfish {

  def apply(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = { 
    fetchMove(dbGame.pgn, initialFen | "", dbGame.aiLevel | 1) map { 
      applyMove(dbGame, _)
    }
  }

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
    http(urlObj <<? Map(
      "pgn" -> pgn,
      "initialFen" -> initialFen,
      "level" -> level.toString
    ) as_str)
  }
}
