package lila.socket

import akka.actor._
import akka.pattern.{ ask, pipe }
import com.typesafe.config.Config

import actorApi._
import makeTimeout.short

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lila.common.Scheduler) {

  import scala.concurrent.duration._

  private val HubName = config getString "hub.name"
  private val MoveBroadcastName = config getString "move_broadcast.name"

  private val socketHub = system.actorOf(Props[SocketHub], name = HubName)

  private val population = system.actorOf(Props[Population])

  system.actorOf(Props[MoveBroadcast], name = MoveBroadcastName)

  system.actorOf(Props[UserRegister])

  scheduler.once(5 seconds) {
    scheduler.message(4 seconds) { socketHub -> actorApi.Broom }
    scheduler.message(1.02 seconds) { population -> PopulationTell }
  }
}

object Env {

  lazy val current = "[boot] socket" describes new Env(
    config = lila.common.PlayApp loadConfig "socket",
    system = lila.common.PlayApp.system,
    scheduler = lila.common.PlayApp.scheduler)
}
