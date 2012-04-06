package lila.system
package ai

import lila.chess.{ Game, Move }
import model.DbGame

import scalaz.effects._
import dispatch._

final class RemoteAi(remoteUrl: String) extends Ai with FenBased {

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

  private def fetchNewFen(oldFen: String, level: Int): IO[String] = io {
    http(urlObj <<? Map(
      "fen" -> oldFen,
      "level" -> level.toString
    ) as_str)
  }

  def health: IO[Boolean] = fetchNewFen(
    oldFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq",
    level = 1
  ).catchLeft map (_.isRight)
}
