package lila.pool

final class Env(
    system: akka.actor.ActorSystem,
    onStart: String => Unit) {

  lazy val api = new PoolApi(
    configs = PoolList.all,
    gameStarter,
    system)

  private lazy val gameStarter = new GameStarter(
    bus = system.lilaBus,
    onStart = onStart)
}

object Env {

  lazy val current: Env = "pool" boot new Env(
    system = lila.common.PlayApp.system,
    onStart = lila.game.Env.current.onStart)
}
