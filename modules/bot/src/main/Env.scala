package lila.bot

import akka.actor._

final class Env(
    system: ActorSystem,
    hub: lila.hub.Env,
    challengeJsonView: lila.challenge.JsonView,
    lightUserApi: lila.user.LightUserApi
) {

  lazy val jsonView = new BotJsonView(lightUserApi)

  lazy val gameStateStream = new GameStateStream(system, jsonView)

  lazy val eventStream = new BotEventStream(system, challengeJsonView)

  lazy val player = new BotPlayer(
    roundMap = hub.actor.roundMap
  )
}

object Env {

  lazy val current: Env = "bot" boot new Env(
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    challengeJsonView = lila.challenge.Env.current.jsonView,
    lightUserApi = lila.user.Env.current.lightUserApi
  )
}
