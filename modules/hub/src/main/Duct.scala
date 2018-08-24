package lila.hub

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.stm._

import lila.base.LilaException

/*
 * Sequential like an actor, but for async functions,
 * and using an STM backend instead of akka actor.
 */
trait Duct {

  type ReceiveAsync = PartialFunction[Any, Fu[Any]]

  // implement async behaviour here
  protected val process: ReceiveAsync

  def !(msg: Any): Unit = atomic { implicit txn =>
    stateRef.transform {
      _.map(_ enqueue msg) orElse {
        Txn.afterCommit { _ => run(msg) }
        Duct.emptyQueue
      }
    }
  }

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: Ref[Option[Queue[Any]]] = Ref(None)

  private[this] def run(msg: Any): Unit =
    process.applyOrElse(msg, Duct.fallback) onComplete { _ => postRun }

  private[this] def postRun = atomic { implicit txn =>
    stateRef.transform {
      _.flatMap(_.dequeueOption) map {
        case (msg, newQueue) =>
          Txn.afterCommit { _ => run(msg) }
          newQueue
      }
    }
  }
}

object Duct {

  private val emptyQueue: Option[Queue[Any]] = Some(Queue.empty)

  private val fallback = { msg: Any =>
    lila.log("Duct").warn(s"unhandled msg: $msg")
    funit
  }
}
