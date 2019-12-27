package lila.common

import akka.actor._

object Lilakka {

  def shutdown(cs: CoordinatedShutdown, phase: CoordinatedShutdown.type => String, name: String)(
      f: () => Funit
  ): Unit =
    cs.addTask(phase(CoordinatedShutdown), name) { () =>
      lila.log("shutdown").info(name)
      Chronometer(f()) pp name inject akka.Done
    }
}
