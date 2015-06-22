package lila.hub

import scala.util.Try
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.pipe

trait SequentialProvider extends Actor {

  import SequentialProvider.Envelope

  type ReceiveAsync = PartialFunction[Any, Fu[_]]

  def process: ReceiveAsync

  def debug = false
  lazy val name = ornicar.scalalib.Random nextString 4

  val windowCount = new lila.common.WindowCount(1 second)

  private def idle: Receive = {

    case msg =>
      context become busy
      processThenDone(Envelope(msg, sender))
  }

  private def busy: Receive = {

    case Done => dequeue match {
      case None => context become idle
      case Some(envelope) =>
        debugQueue
        processThenDone(envelope)
    }

    case msg =>
      queue enqueue Envelope(msg, sender)
      debugQueue
  }

  def receive = idle

  private val queue = collection.mutable.Queue[Envelope]()
  private def dequeue: Option[Any] = Try(queue.dequeue).toOption

  private def debugQueue {
    if (debug) queue.size match {
      case size if (size == 50 || (size >= 100 && size % 100 == 0)) =>
        logwarn(s"Seq[$name] queue = $size, mps = ${windowCount.get}")
      case _ =>
    }
  }

  private case object Done

  private def fallback: ReceiveAsync = {
    case _ => fuccess(Status.Failure)
  }

  private def processThenDone(signal: Any) {
    windowCount.add
    signal match {
      // we don't want to send Done after actor death
      case SequentialProvider.Terminate => self ! PoisonPill
      case Envelope(msg, replyTo)       => (process orElse fallback)(msg) pipeTo replyTo andThenAnyway { self ! Done }
      case x                            => logwarn(s"SequentialProvider should never have received $x")
    }
  }
}

object SequentialProvider {

  case class Envelope(msg: Any, replyTo: ActorRef)

  case object Terminate
}
