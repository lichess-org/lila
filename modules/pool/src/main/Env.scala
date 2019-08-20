package lidraughts.pool

import scala.concurrent.duration._

import lidraughts.hub.FutureSequencer

final class Env(
    system: akka.actor.ActorSystem,
    playbanApi: lidraughts.playban.PlaybanApi
) {

  private lazy val hookThieve = new HookThieve(system.lidraughtsBus)

  lazy val api = new PoolApi(
    configs = PoolList.all,
    hookThieve = hookThieve,
    gameStarter = gameStarter,
    playbanApi = playbanApi,
    system = system
  )

  private lazy val gameStarter = new GameStarter(
    bus = system.lidraughtsBus,
    onStart = gameId => system.lidraughtsBus.publish(gameId, 'gameStartId),
    sequencer = new FutureSequencer(
      system = system,
      executionTimeout = 5.seconds.some,
      logger = logger
    )
  )
}

object Env {

  lazy val current: Env = "pool" boot new Env(
    system = lidraughts.common.PlayApp.system,
    playbanApi = lidraughts.playban.Env.current.api
  )
}
