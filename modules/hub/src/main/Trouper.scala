package lila.hub

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import lila.hub.actorApi.Shutdown
import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }

/*
 * Like an actor, but not an actor.
 * Uses an Atomic Reference backend for sequentiality.
 * Has an unbounded (!) Queue of messages.
 * Like Duct, but for synchronous message processors.
 */
trait Trouper extends lila.common.Tellable {

  import Trouper._

  // implement async behaviour here
  protected val process: Receive

  protected var isAlive = true

  def getIsAlive = isAlive

  def stop(): Unit = {
    isAlive = false
  }

  def !(msg: Any): Unit =
    if (isAlive && stateRef.getAndUpdate(
      new UnaryOperator[State] {
        override def apply(state: State): State = Some(state.fold(Queue.empty[Any])(_ enqueue msg))
      }
    ).isEmpty) run(msg)

  def ask[A](makeMsg: Promise[A] => Any): Fu[A] = {
    val promise = Promise[A]
    this ! makeMsg(promise)
    promise.future
  }

  def queueSize = stateRef.get().fold(0)(_.size + 1)

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[State] = new AtomicReference(None)

  private[this] def run(msg: Any): Unit = Future {
    process.applyOrElse(msg, fallback)
  } onComplete postRun

  private[this] val postRun = (_: Any) =>
    stateRef.getAndUpdate(postRunUpdate) flatMap (_.headOption) foreach run

  private val fallback: Receive = {
    case Shutdown => stop()
    case msg => lila.log("trouper").warn(s"unhandled msg: $msg")
  }

  lazy val uniqueId = Integer.toHexString(hashCode)
}

object Trouper {

  type Receive = PartialFunction[Any, Unit]

  private type State = Option[Queue[Any]]

  private val postRunUpdate = new UnaryOperator[State] {
    override def apply(state: State): State =
      state flatMap { q =>
        if (q.isEmpty) None else Some(q.tail)
      }
  }
}
