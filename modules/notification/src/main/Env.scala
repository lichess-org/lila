package lila.notification

import akka.actor.ActorRef

final class Env(socketHub: lila.hub.ActorLazyRef, renderer: lila.hub.ActorLazyRef) {

  lazy val api = new Api(socketHub, renderer)
}

object Env {

  lazy val current = "[boot] notification" describes new Env(
    socketHub = lila.hub.Env.current.socket.hub,
    renderer = lila.hub.Env.current.actor.renderer)
}
