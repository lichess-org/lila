package lila.hub

import scala.concurrent.duration._
import scala.util.Try

import akka.actor._

final class Sequencer(receiveTimeout: FiniteDuration) extends Actor {

  context setReceiveTimeout receiveTimeout

  private def idle: Receive = {

    case msg =>
      context become busy
      processThenDone(msg)
  }

  private def busy: Receive = {

    case Done => dequeue match {
      case None       => context become idle
      case Some(work) => processThenDone(work)
    }

    case msg => queue enqueue msg
  }

  def receive = idle

  private val queue = collection.mutable.Queue[Any]()
  private def dequeue: Option[Any] = Try(queue.dequeue).toOption

  private case object Done

  private def processThenDone(work: Any) {
    work match {
      case ReceiveTimeout => self ! PoisonPill
      case Sequencer.Work(run) => run() andThenAnyway { self ! Done }
      case x => play.api.Logger("Sequencer").warn(s"Unsupported message $x")
    }
  }
}

object Sequencer {

  case class Work(run: () => Funit)

  def work(run: => Funit): Work = Work(() => run)
}
