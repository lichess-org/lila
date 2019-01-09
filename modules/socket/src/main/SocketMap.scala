package lila.socket

import scala.concurrent.duration._
import ornicar.scalalib.Random.approximatly

import lila.hub.{ Trouper, TrouperMap }

object SocketMap {

  def apply[T <: Trouper](
    system: akka.actor.ActorSystem,
    mkTrouper: String => T,
    accessTimeout: FiniteDuration,
    monitoringName: String,
    broomFrequency: FiniteDuration
  ): TrouperMap[T] = {

    val trouperMap = new TrouperMap[T](
      mkTrouper = mkTrouper,
      accessTimeout = accessTimeout
    )

    system.scheduler.schedule(30 seconds, 30 seconds) {
      trouperMap.monitor(monitoringName)
    }
    system.scheduler.schedule(approximatly(0.1f)(12.seconds.toMillis).millis, broomFrequency) {
      trouperMap tellAll actorApi.Broom
    }
    system.lilaBus.subscribeFun('shutdown) { case _ => trouperMap.killAll }

    trouperMap
  }
}
