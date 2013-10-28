package lila.notification

import akka.actor.ActorSelection

final class Env(bus: lila.common.Bus, renderer: ActorSelection) {

  lazy val api = new Api(bus, renderer)
}

object Env {

  lazy val current = "[boot] notification" describes new Env(
    bus = lila.common.PlayApp.system.lilaBus,
    renderer = lila.hub.Env.current.actor.renderer)
}
