package lila.notification

import akka.actor.ActorSelection

final class Env(socketHub: ActorSelection, renderer: ActorSelection) {

  lazy val api = new Api(socketHub, renderer)
}

object Env {

  lazy val current = "[boot] notification" describes new Env(
    socketHub = lila.hub.Env.current.socket.hub,
    renderer = lila.hub.Env.current.actor.renderer)
}
