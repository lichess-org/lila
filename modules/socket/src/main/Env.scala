package lila.socket

import akka.actor._
import com.typesafe.config.Config

import actorApi._

final class Env(
    system: ActorSystem
) {

  import scala.concurrent.duration._

  private val population = new Population(system)

  private val moveBroadcast = new MoveBroadcast(system)

  private val userRegister = new UserRegister(system)

  system.scheduler.schedule(5 seconds, 1 seconds) { population ! PopulationTell }
}

object Env {

  lazy val current = "socket" boot new Env(
    system = lila.common.PlayApp.system
  )
}
