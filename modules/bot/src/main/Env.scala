package lila.bot

import akka.actor._

final class Env(
    system: ActorSystem,
    lightUserApi: lila.user.LightUserApi
) {

  lazy val jsonView = new BotJsonView(lightUserApi)

  lazy val gameStateStream = new GameStateStream(system, jsonView)
}

object Env {

  lazy val current: Env = "bot" boot new Env(
    system = lila.common.PlayApp.system,
    lightUserApi = lila.user.Env.current.lightUserApi
  )
}
