package lila.pool

import scala.concurrent.duration._

import lila.hub.FutureSequencer
import lila.common.Bus

final class Env(
    system: akka.actor.ActorSystem,
    playbanApi: lila.playban.PlaybanApi
) {

  private lazy val hookThieve = new HookThieve()(system)

  lazy val api = new PoolApi(
    configs = PoolList.all,
    hookThieve = hookThieve,
    gameStarter = gameStarter,
    playbanApi = playbanApi,
    system = system
  )

  private lazy val gameStarter = new GameStarter(
    onStart = gameId => Bus.publish(lila.game.Game.Id(gameId), "gameStartId"),
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
    playbanApi = lila.playban.Env.current.api
  )
}
