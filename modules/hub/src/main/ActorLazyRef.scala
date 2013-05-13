package lila.hub

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

// like ActorRef, but can be created before the referenced actor exists
// like ActorSelection, but supports ask pattern
final class ActorLazyRef(system: ActorSystem, path: ActorPath) {

  def ref = system.actorFor("/user/" + path)

  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender) { ref ! msg }

  def ?(msg: Any)(implicit timeout: Timeout): Fu[Any] = ref ? msg
}

object ActorLazyRef {

  def apply(system: ActorSystem)(path: String): ActorLazyRef = 
    new ActorLazyRef(system, ActorPath fromString path)
}
