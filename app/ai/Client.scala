package lila
package ai

import scalaz.effects._
import akka.dispatch.Future

trait Client extends Ai {

  val playUrl: String

  protected def tryPing: Future[Int]

  // tells whether the remote AI is healthy or not
  // frequently updated by a scheduled actor
  protected var ping = none[Int]
  protected val pingAlert = 5000

  def or(fallback: Ai) = if (currentHealth) this else fallback

  def currentPing = ping
  def currentHealth = isHealthy(ping)

  def diagnose: Unit = tryPing onComplete {
    case Left(e) ⇒ {
      println("remote AI error: " + e.getMessage)
      changePing(none)
    }
    case Right(p) ⇒ changePing(p.some)
  }

  private def changePing(p: Option[Int]) = {
    if (isHealthy(p) && !currentHealth)
      println("remote AI is up, ping = " + p)
    else if (!isHealthy(p) && currentHealth)
      println("remote AI is down, ping = " + p)
    ping = p
  }

  private def isHealthy(p: Option[Int]) = p.fold(isFast, false)

  private def isFast(p: Int) = p < pingAlert
}
