package lidraughts.pool

import scala.concurrent.duration._

import lidraughts.hub.FutureSequencer

final class Env(
    lobbyActor: akka.actor.ActorSelection,
    playbanApi: lidraughts.playban.PlaybanApi,
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
    bus = system.lidraughtsBus,
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
    lobbyActor = lidraughts.hub.Env.current.actor.lobby,
    playbanApi = lidraughts.playban.Env.current.api,
    system = lidraughts.common.PlayApp.system,
    onStart = lidraughts.game.Env.current.onStart
  )
}
