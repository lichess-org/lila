package lila.common

import akka.actor._
import scala.concurrent.duration._

final class Debouncer[A: Manifest](timeout: FiniteDuration, function: A => Unit) extends Actor {

  private var scheduled: Cancellable = new Cancellable {
    def cancel() = true
    def isCancelled = true
  }

  private case class DoItNow(a: A)

  def receive = {

    case a: A =>
      if (!scheduled.isCancelled) scheduled.cancel()
      scheduled = context.system.scheduler.scheduleOnce(timeout, self, DoItNow(a))

    case DoItNow(a) => function(a)

    case a => println("debouncer unprocessed " + a)
  }
}

object Debouncer {

  sealed trait Nothing
  object Nothing extends Nothing
}
