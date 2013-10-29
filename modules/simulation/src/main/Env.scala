package lila.simulation

import scala.concurrent.duration._

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    lobbyEnv: lila.lobby.Env,
    roundEnv: lila.round.Env,
    system: ActorSystem) {

  private lazy val simulator = system.actorOf(Props(new Simulator(
    lobbyEnv = lobbyEnv,
    roundEnv = roundEnv
  )), name = "simulator")

  def start {
    system.scheduler.scheduleOnce(2 seconds, simulator, actorApi.Start)
  }
}

object Env {

  lazy val current = "[boot] simulation" describes new Env(
    config = lila.common.PlayApp loadConfig "simulation",
    lobbyEnv = lila.lobby.Env.current,
    roundEnv = lila.round.Env.current,
    system = lila.common.PlayApp.system)
}
