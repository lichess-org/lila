package lila.bot

import akka.actor._

final class Env(
    system: ActorSystem,
    hub: lila.hub.Env,
    lightUserApi: lila.user.LightUserApi
) {

  lazy val jsonView = new BotJsonView(lightUserApi)

  lazy val gameStateStream = new GameStateStream(system, jsonView)

  lazy val eventStream = new BotEventStream(system, jsonView)

  lazy val player = new BotPlayer(
    roundMap = hub.actor.roundMap
  )
}

object Env {

  lazy val current: Env = "bot" boot new Env(
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    lightUserApi = lila.user.Env.current.lightUserApi
  )
}
