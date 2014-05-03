package lila.simulation

import scala.concurrent.duration._

import akka.actor._

final class Env(
    typesafeConfig: com.typesafe.config.Config,
    featured: lila.tv.Featured,
    lobbyEnv: lila.lobby.Env,
    roundEnv: lila.round.Env,
    system: ActorSystem) {

  private val config = Config(
    players = typesafeConfig getInt "players",
    watchers = typesafeConfig getInt "watchers")

  private lazy val simulator = system.actorOf(Props(new Simulator(
    config = config,
    featured = featured,
    lobbyEnv = lobbyEnv,
    roundEnv = roundEnv
  )), name = "simulator")

  def start {
    system.scheduler.scheduleOnce(2 seconds, simulator, Simulator.Start)
  }
}

object Env {

  lazy val current = "[boot] simulation" describes new Env(
    typesafeConfig = lila.common.PlayApp loadConfig "simulation",
    featured = lila.tv.Env.current.featured,
    lobbyEnv = lila.lobby.Env.current,
    roundEnv = lila.round.Env.current,
    system = lila.common.PlayApp.system)
}
