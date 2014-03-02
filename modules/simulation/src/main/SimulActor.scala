package lila.simulation

import scala.concurrent.duration._

import akka.actor._

private[simulation] trait SimulActor extends Actor {

  def name: String
  def logging = false

  def log(msg: Any) {
    if (logging) println(s"${name} ${msg}")
  }

  def delay(duration: FiniteDuration)(action: => Unit) {
    context.system.scheduler.scheduleOnce(duration)(action)
  }

  def delayRandomMillis(max: Int)(action: => Unit) {
    delay((10 + scala.util.Random.nextInt(max)).millis)(action)
  }
}
