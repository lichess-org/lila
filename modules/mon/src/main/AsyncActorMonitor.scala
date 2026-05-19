package lila.mon

import play.api.Logger

object asyncActorMonitor:

  private lazy val logger = Logger("asyncActor")

  lazy val full = scalalib.actor.AsyncActorBounded.Monitor(
    overflow = name =>
      lila.mon.asyncActor.overflow(name).increment()
      logger.warn(s"[$name] queue is full")
    ,
    queueSize = (name, size) => lila.mon.asyncActor.queueSize(name).record(size),
    unhandled = (name, msg) => logger.warn(s"[$name] unhandled msg: $msg")
  )

  lazy val highCardinality = full.copy(
    queueSize = (_, _) => ()
  )

  lazy val unhandled = full.copy(
    overflow = _ => (),
    queueSize = (_, _) => ()
  )
