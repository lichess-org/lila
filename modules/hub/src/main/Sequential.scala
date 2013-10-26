package lila.hub

import scala.util.Try

import akka.actor._

trait SequentialActor extends Actor {

  def process(msg: Any): Funit

  def idle: Receive = {

    case msg ⇒ {
      context become busy
      processThenDone(msg)
    }
  }

  def busy: Receive = {

    case Done ⇒ dequeue match {
      case None      ⇒ context become idle
      case Some(msg) ⇒ processThenDone(msg)
    }

    case msg ⇒ queue enqueue msg
  }

  def receive = idle

  private val queue = collection.mutable.Queue[Any]()
  private def dequeue: Option[Any] = Try(queue.dequeue).toOption

  private case object Done

  private def processThenDone(msg: Any) {
    process(msg) >>- { self ! Done }
  }
}
