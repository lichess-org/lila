package lila.pool

import scala.concurrent.duration._

import akka.actor._

final class Env(
    system: akka.actor.ActorSystem,
    onStart: String => Unit) {

  lazy val api = new PoolApi(
    configs = PoolList.all,
    gameStarter,
    system)

  private lazy val gameStarter = new GameStarter(
    bus = system.lilaBus,
    onStart = onStart,
    sequencer = system.actorOf(Props(
      classOf[lila.hub.Sequencer], none, 10.seconds.some, logger
    ), name = "pool-sequencer")
  )
}

object Env {

  lazy val current: Env = "pool" boot new Env(
    system = lila.common.PlayApp.system,
    onStart = lila.game.Env.current.onStart)
}
