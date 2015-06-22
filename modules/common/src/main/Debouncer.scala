package lila.common

import akka.actor._
import scala.concurrent.duration._

// do NOT embed me in an actor
// for it would likely create a memory leak
final class Debouncer[A: Manifest](length: FiniteDuration, function: A => Unit) extends Actor {

  private case object DelayEnd

  private var delayed: Option[A] = none

  def ready: Receive = {

    case a: A =>
      function(a)
      context.system.scheduler.scheduleOnce(length, self, DelayEnd)
      context become delay
  }

  def delay: Receive = {

    case a: A => delayed = a.some

    case DelayEnd =>
      context become ready
      delayed foreach { a =>
        self ! a
        delayed = none
      }
  }

  def receive = ready
}

object Debouncer {

  sealed trait Nothing
  object Nothing extends Nothing
}
