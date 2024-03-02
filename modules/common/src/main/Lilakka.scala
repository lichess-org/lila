package lila.common

import akka.actor.*

object Lilakka:

  val shutdownLogger = lila.log("shutdown")

  def shutdown(cs: CoordinatedShutdown, makePhase: CoordinatedShutdown.type => String, name: String)(
      f: () => Funit
  ): Unit =
    val phase = makePhase(CoordinatedShutdown)
    val msg   = s"$phase $name"
    cs.addTask(phase, name): () =>
      shutdownLogger.info(msg)
      Chronometer(f())
        .log(shutdownLogger)(_ => msg)
        .result
        .inject(akka.Done)
