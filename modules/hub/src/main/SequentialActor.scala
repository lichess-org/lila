package lila.hub

import scala.concurrent.duration._
import scala.util.Try

import akka.actor._

import lila.common.LilaException

trait SequentialActor extends Actor {

  type ReceiveAsync = PartialFunction[Any, Funit]

  def process: ReceiveAsync

  def futureTimeout: Option[FiniteDuration] = none

  private def idle: Receive = {

    case msg =>
      context become busy
      processThenDone(msg)
  }

  private def busy: Receive = {

    case Done => dequeue match {
      case None      => context become idle
      case Some(msg) => processThenDone(msg)
    }

    case msg => queue enqueue msg
  }

  def receive = idle

  def onFailure(e: Exception) {}

  private val queue = collection.mutable.Queue[Any]()
  private def dequeue: Option[Any] = Try(queue.dequeue).toOption

  private case object Done

  private def fallback: ReceiveAsync = {
    case _ => funit
  }

  private def processThenDone(work: Any) {
    work match {
      // we don't want to send Done after actor death
      case SequentialActor.Terminate => self ! PoisonPill
      case msg =>
        val future = (process orElse fallback)(msg)
        futureTimeout.fold(future) { timeout =>
          future.withTimeout(timeout, LilaException(s"Sequential actor timeout: $timeout"))(context.system)
        }.addFailureEffect(onFailure).andThenAnyway { self ! Done }
    }
  }
}

object SequentialActor {

  case object Terminate
}
