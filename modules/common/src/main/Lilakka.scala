package lila.common

import akka.actor.*

object Lilakka:

  def shutdown(cs: CoordinatedShutdown, makePhase: CoordinatedShutdown.type => String, name: String)(
      f: () => Funit
  ): Unit =
    val phase = makePhase(CoordinatedShutdown)
    val msg = s"$phase $name"
    cs.addTask(phase, name): () =>
      lila.log.system.info(s"shutdown $msg")
      f().dmap: _ =>
        lila.log.system.info(s"shutdown $msg done")
        akka.Done
