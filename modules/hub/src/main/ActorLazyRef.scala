package lila.hub

import akka.actor._

final class ActorLazyRef(system: ActorSystem, path: ActorPath) {

  def ref = system.actorFor("/user/" + path)

  def !(
}

object ActorLazyRef {

  def apply(system: ActorSystem)(path: String): ActorLazyRef = 
    new ActorLazyRef(system, ActorPath fromString path)
}
