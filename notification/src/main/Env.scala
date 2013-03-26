package lila.notification

import com.typesafe.config.Config
import akka.actor.ActorRef

final class Env(
    sockets: ActorRef,
    renderer: ActorRef) {

  lazy val api = new Api(sockets, renderer)
}

object Env {

  lazy val current = new Env(
    sockets = lila.hub.Env.current.sockets,
    renderer = lila.hub.Env.current.actor.renderer)
}
