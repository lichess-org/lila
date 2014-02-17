package lila.ai

import scala.concurrent.Future
import scala.util.{ Success, Failure }

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
    case Failure(e) => {
      logwarn("remote AI error: " + e.getMessage)
      changePing(none)
    }
    case Success(p) => changePing(p.some)
  }

  private def changePing(p: Option[Int]) = {
    if (isHealthy(p) && !currentHealth)
      loginfo("remote AI is up, ping = " + p)
    else if (!isHealthy(p) && currentHealth)
      logerr("remote AI is down, ping = " + p)
    ping = p
  }

  private def isHealthy(p: Option[Int]) = p ?? isFast

  private def isFast(p: Int) = p < pingAlert
}
