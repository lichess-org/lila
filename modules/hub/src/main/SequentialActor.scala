package lila.hub

import scala.util.Try

import akka.actor._

trait SequentialActor extends Actor {

  type ReceiveAsync = PartialFunction[Any, Funit]

  def process: ReceiveAsync

  def idle: Receive = {

    case msg => {
      context become busy
      processThenDone(msg)
    }
  }

  def busy: Receive = {

    case Done => {
      dequeue match {
        case None      => context become idle
        case Some(msg) => processThenDone(msg)
      }
    }

    case msg => {
      queue enqueue msg
    }
  }

  def receive = idle

  private val queue = collection.mutable.Queue[Any]()
  private def dequeue: Option[Any] = Try(queue.dequeue).toOption

  private case object Done

  private def fallback: ReceiveAsync = {
    case work => fuccess()
  }

  private def processThenDone(work: Any) {
    work match {
      // we don't want to send Done after actor death
      case SequentialActor.Terminate => self ! PoisonPill
      case msg => {
        (process orElse fallback)(msg) >>- 
        (self ! Done)
      }
    }
  }
}

object SequentialActor {

  case object Terminate
}
