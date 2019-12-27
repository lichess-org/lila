package lila.common

import akka.actor._

object Lilakka {

  def shutdown(cs: CoordinatedShutdown, makePhase: CoordinatedShutdown.type => String, name: String)(
      f: () => Funit
  ): Unit = {
    val phase = makePhase(CoordinatedShutdown)
    val msg   = s"$phase $name"
    cs.addTask(phase, name) { () =>
      lila.log("shutdown").info(msg)
      Chronometer(f()) pp msg inject akka.Done
    }
  }
}
