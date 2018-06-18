package lila.pool

import scala.concurrent.duration._

import akka.actor._

final class Env(
    lobbyActor: ActorSelection,
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
    sequencer = system.actorOf(Props(
      classOf[lila.hub.Sequencer], none, 10.seconds.some, logger
    ), name = "pool-sequencer")
  )
}

object Env {

  lazy val current: Env = "pool" boot new Env(
    lobbyActor = lila.hub.Env.current.actor.lobby,
    playbanApi = lila.playban.Env.current.api,
    system = old.play.Env.actorSystem,
    onStart = lila.game.Env.current.onStart
  )
}
