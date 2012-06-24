package lila
package ai

import chess.{ Game, Move }
import game.DbGame

import scalaz.effects._
import dispatch.{ Http, NoLogging, url }
import dispatch.thread.{ Safety ⇒ ThreadSafety }

final class RemoteAi(
    remoteUrl: String) extends Ai with FenBased {

  // tells whether the remote AI is healthy or not
  // frequently updated by a scheduled actor
  private var ping = none[Int]
  val pingAlert = 3000

  private lazy val http = new Http with ThreadSafety with NoLogging
  private lazy val urlObj = url(remoteUrl)

  def apply(dbGame: DbGame, initialFen: Option[String]): IO[Valid[(Game, Move)]] = {

    val oldGame = dbGame.toChess
    val oldFen = toFen(oldGame, dbGame.variant)

    fetchNewFen(oldFen, dbGame.aiLevel | 1) map { newFen ⇒
      applyFen(oldGame, newFen)
    }
  }

  def or(fallback: Ai) = if (currentHealth) this else fallback

  def currentPing = ping
  def currentHealth = ping.fold(_ < pingAlert, false)

  def diagnose: IO[Unit] = for {
    p ← tryPing
    _ ← p.fold(_ < pingAlert, false).fold(
      currentHealth.fold(io(), putStrLn("remote AI is up, ping = " + p)),
      putStrLn("remote AI is down, ping = " + p))
    _ ← io { ping = p }
  } yield ()

  private def fetchNewFen(oldFen: String, level: Int): IO[String] = io {
    http(urlObj <<? Map(
      "fen" -> oldFen,
      "level" -> level.toString
    ) as_str)
  }

  private def tryPing: IO[Option[Int]] = for {
    start ← io(nowMillis)
    received ← fetchNewFen(
      oldFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq",
      level = 1
    ).catchLeft map (_.isRight)
    delay ← io(nowMillis - start)
  } yield received option delay.toInt
}
