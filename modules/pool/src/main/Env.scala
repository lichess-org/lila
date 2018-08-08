package lidraughts.pool

import scala.concurrent.duration._

import akka.actor._

final class Env(
    lobbyActor: ActorSelection,
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
    sequencer = system.actorOf(Props(
      classOf[lidraughts.hub.Sequencer], none, 10.seconds.some, logger
    ), name = "pool-sequencer")
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
