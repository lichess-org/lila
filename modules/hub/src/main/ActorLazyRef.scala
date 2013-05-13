package lila.hub

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

// like ActorRef, but can be created before the referenced actor exists
// like ActorSelection, but supports ask pattern
final class ActorLazyRef private (system: ActorSystem, path: String) {

  def ref = system actorFor path

  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender) { ref ! msg }

  def ?(msg: Any)(implicit timeout: Timeout): Fu[Any] = ref ? msg
}

object ActorLazyRef {

  def apply(system: ActorSystem)(path: String): ActorLazyRef = 
    new ActorLazyRef(system, "/user/" + path)
}
