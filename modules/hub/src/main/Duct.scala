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

  import Duct.State

  def !(msg: Any): Unit = atomic { implicit txn =>
    stateRef.transform { state =>
      if (state.busy) state enqueue msg
      else {
        Txn.afterCommit { _ => run(msg) }
        state.copy(busy = true)
      }
    }
  }

  private[this] val stateRef = Ref(State(false, Queue.empty))

  private[this] def run(msg: Any): Fu[Any] =
    process.applyOrElse(msg, Duct.fallback) addEffectAnyway postRun

  private[this] def postRun = atomic { implicit txn =>
    stateRef.transform { state =>
      state.queue.dequeueOption match {
        case None => state.copy(busy = false)
        case Some((msg, newQueue)) =>
          Txn.afterCommit { _ => run(msg) }
          state.copy(busy = true, queue = newQueue)
      }
    }
  }
}

object Duct {

  case class State(busy: Boolean, queue: Queue[Any]) {
    def enqueue(msg: Any) = copy(queue = queue enqueue msg)
  }

  private val fallback = { msg: Any =>
    lila.log("Duct").warn(s"unhandled msg: $msg")
    funit
  }
}
