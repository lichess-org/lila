package lila.pool

import scala.concurrent.duration._

import lila.hub.FutureSequencer

final class Env(
    system: akka.actor.ActorSystem,
    playbanApi: lila.playban.PlaybanApi,
    onStart: String => Unit
) {

  private lazy val hookThieve = new HookThieve(system.lilaBus)

  lazy val api = new PoolApi(
    configs = PoolList.all,
    hookThieve = hookThieve,
    gameStarter = gameStarter,
    playbanApi = playbanApi,
    system = system
  )

  private lazy val gameStarter = new GameStarter(
    bus = system.lilaBus,
    onStart = onStart,
    sequencer = new FutureSequencer(
      system = system,
      executionTimeout = 5.seconds.some,
      logger = logger
    )
  )
}

object Env {

  lazy val current: Env = "pool" boot new Env(
    system = lila.common.PlayApp.system,
    playbanApi = lila.playban.Env.current.api,
    onStart = lila.game.Env.current.onStart
  )
}
