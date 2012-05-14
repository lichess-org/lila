package lila
package ai

import lila.chess.{ Game, Move }
import game.DbGame

import scalaz.effects._
import dispatch._

final class RemoteAi(
    remoteUrl: String) extends Ai with FenBased {

  // tells whether the remote AI is healthy or not
  // frequently updated by a scheduled actor
  private var health = false

  private lazy val http = new Http with thread.Safety {
    override def make_logger = new Logger {
      def info(msg: String, items: Any*) {}
      def warn(msg: String, items: Any*) { println("WARN: " + msg.format(items: _*)) }
    }
  }
  private lazy val urlObj = url(remoteUrl)

  def apply(dbGame: DbGame): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess
    val oldFen = toFen(oldGame, dbGame.variant)

    fetchNewFen(oldFen, dbGame.aiLevel | 1) map { newFen ⇒
      applyFen(oldGame, newFen)
    }
  }

  def or(fallback: Ai) = if (health) this else fallback

  def currentHealth = health

  def diagnose: IO[Unit] = for {
    h ← healthCheck
    _ ← h.fold(
      health.fold(io(), putStrLn("remote AI is up")),
      putStrLn("remote AI is down"))
    _ ← io { health = h }
  } yield ()

  private def fetchNewFen(oldFen: String, level: Int): IO[String] = io {
    http(urlObj <<? Map(
      "fen" -> oldFen,
      "level" -> level.toString
    ) as_str)
  }

  private def healthCheck: IO[Boolean] = fetchNewFen(
    oldFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq",
    level = 1
  ).catchLeft map (_.isRight)
}
