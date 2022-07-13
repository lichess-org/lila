package lila.api

import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

final private class Monitoring(implicit ec: ExecutionContext, system: ActorSystem) {

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size).unit
  }
}
