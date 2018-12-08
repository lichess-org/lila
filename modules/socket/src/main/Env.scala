package lidraughts.socket

import akka.actor._
import com.typesafe.config.Config

import actorApi._

final class Env(
    config: Config,
    system: ActorSystem,
    scheduler: lidraughts.common.Scheduler
) {

  import scala.concurrent.duration._

  private val socketHub = system.actorOf(Props[SocketHub])

  private val population = new Population(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  scheduler.once(10 seconds) {
    scheduler.message(4 seconds) { socketHub -> actorApi.Broom }
  }
  system.scheduler.schedule(5 seconds, 1 seconds) { population ! PopulationTell }
}

object Env {

  lazy val current = "socket" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "socket",
    system = lidraughts.common.PlayApp.system,
    scheduler = lidraughts.common.PlayApp.scheduler
  )
}
