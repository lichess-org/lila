package lila.hub

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
    current.transform(_ >> process.applyOrElse(msg, Duct.fallback))
  }

  private[this] val current: Ref[Fu[Any]] = Ref(funit)
}

object Duct {

  private val fallback = { msg: Any =>
    lila.log("Duct").warn(s"unhandled msg: $msg")
    funit
  }
}
