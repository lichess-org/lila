package lila.hub

import scala.concurrent.duration._

import akka.actor._

import lila.base.LilaException

trait SequentialActor extends Actor {

  type ReceiveAsync = PartialFunction[Any, Funit]

  def process: ReceiveAsync

  def futureTimeout: Option[FiniteDuration] = None

  private val idle: Receive = {
    case msg =>
      context become busy
      processThenDone(msg)
  }

  private[this] val queue = new java.util.ArrayDeque[Any]

  private val busy: Receive = {
    case Done => queue.poll match {
      case null => context become idle
      case savedMsg => processThenDone(savedMsg)
    }

    case newMsg => queue addLast newMsg
  }

  def receive = idle

  def onFailure(e: Exception): Unit = {}

  private case object Done

  private def processThenDone(work: Any): Unit = {
    work match {
      // we don't want to send Done after actor death
      case SequentialActor.Terminate => self ! PoisonPill
      case msg =>
        val future = process.applyOrElse(msg, SequentialActor.fallback)
        futureTimeout.fold(future) { timeout =>
          future.withTimeout(timeout, LilaException(s"Sequential actor timeout: $timeout"))(context.system)
        }.addFailureEffect(onFailure).addEffectAnyway { self ! Done }
    }
  }
}

object SequentialActor {
  private val fallback = { msg: Any =>
    lila.log("SeqActor").warn(s"unhandled msg: $msg")
    funit
  }

  case object Terminate
}
