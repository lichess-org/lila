package lila.notification

import lila.hub.MetaHub

import com.typesafe.config.Config
import akka.actor.ActorRef

final class Env(
    metahub: ActorRef,
    renderer: ActorRef) {

  lazy val api = new Api(metahub, renderer)
}

object Env {

  lazy val current = new Env
}
