package lila.pool

import scala.concurrent.duration._

import lila.hub.FutureSequencer

final class Env(
    lobbyActor: akka.actor.ActorSelection,
    playbanApi: lila.playban.PlaybanApi,
    system: akka.actor.ActorSystem,
    onStart: String => Unit
) {

  private lazy val hookThieve = new HookThieve(lobbyActor)

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
    lobbyActor = lila.hub.Env.current.lobby,
    playbanApi = lila.playban.Env.current.api,
    system = lila.common.PlayApp.system,
    onStart = lila.game.Env.current.onStart
  )
}
