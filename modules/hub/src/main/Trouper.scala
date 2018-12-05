package lila.hub

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.{ Future, Promise }
import scala.concurrent.stm._

import lila.base.LilaException

/*
 * Like an actor, but not an actor.
 * Uses an STM backend for sequentiality.
 * Has an unbounded (!) Queue of messages.
 * Like Duct, but for synchronous message processors.
 */
trait Trouper {

  // implement async behaviour here
  protected val process: Trouper.Receive

  def start(): Unit
  def stop(): Unit

  def !(msg: Any): Unit =
    if (stateRef.single.getAndTransform { q =>
      Some(q.fold(Queue.empty[Any])(_ enqueue msg))
    } isEmpty) run(msg)

  def queueSize = stateRef.single().??(_.size)

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: Ref[Option[Queue[Any]]] = Ref(None)

  private[this] def run(msg: Any): Unit = Future {
    process.applyOrElse(msg, Trouper.fallback)
  } onComplete postRun

  private[this] val postRun = (_: Any) =>
    stateRef.single.getAndTransform {
      _ flatMap { q =>
        if (q.isEmpty) None else Some(q.tail)
      }
    } flatMap (_.headOption) foreach run
}

object Trouper {

  type Receive = PartialFunction[Any, Any]

  private val fallback = { msg: Any =>
    lila.log("Trouper").warn(s"unhandled msg: $msg")
  }

  case class Ask[T](msg: Any, res: Promise[T])
}
