package lila.notification

import akka.actor.ActorRef

final class Env(sockets: ActorRef, renderer: ActorRef) {

  lazy val api = new Api(sockets, renderer)
}

object Env {

  lazy val current = "[boot] notification" describes new Env(
    sockets = lila.hub.Env.current.sockets,
    renderer = lila.hub.Env.current.actor.renderer)
}
