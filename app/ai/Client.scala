package lila
package ai

import dispatch.{ Http, NoLogging, url }
import dispatch.thread.{ Safety ⇒ ThreadSafety }
import scalaz.effects._

trait Client extends Ai {

  val playUrl: String

  protected def tryPing: IO[Option[Int]] 

  // tells whether the remote AI is healthy or not
  // frequently updated by a scheduled actor
  protected var ping = none[Int]
  protected val pingAlert = 3000

  protected lazy val http = new Http with ThreadSafety with NoLogging
  protected lazy val playUrlObj = url(playUrl)

  def or(fallback: Ai) = if (currentHealth) this else fallback

  def currentPing = ping
  def currentHealth = ping.fold(_ < pingAlert, false)

  val diagnose: IO[Unit] = for {
    p ← tryPing
    _ ← p.fold(_ < pingAlert, false).fold(
      currentHealth.fold(io(), putStrLn("remote AI is up, ping = " + p)),
      putStrLn("remote AI is down, ping = " + p))
    _ ← io { ping = p }
  } yield ()
}
