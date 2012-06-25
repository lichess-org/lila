package lila
package ai
package crafty

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._

final class Client(val playUrl: String) extends ai.Client with FenBased {

  def play(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess
    val oldFen = toFen(oldGame, dbGame.variant)

    fetchNewFen(oldFen, dbGame.aiLevel | 1) map { newFen ⇒
      applyFen(oldGame, newFen)
    }
  }

  def analyse(dbGame: DbGame, initialFen: Option[String]) = 
    throw new RuntimeException("Crafty analysis is not implemented")

  private def fetchNewFen(oldFen: String, level: Int): IO[String] = io {
    http(playUrlObj <<? Map(
      "fen" -> oldFen,
      "level" -> level.toString
    ) as_str)
  }

  protected def tryPing: IO[Option[Int]] = for {
    start ← io(nowMillis)
    received ← fetchNewFen(
      oldFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq",
      level = 1
    ).catchLeft map (_.isRight)
    delay ← io(nowMillis - start)
  } yield received option delay.toInt
}
