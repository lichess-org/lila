package lila.hub

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

// like ActorRef, but can be created before the referenced actor exists
// like ActorSelection, but supports ask pattern
final class ActorLazyRef private (system: ActorSystem, path: String) {

  // def ref = system actorFor path
  // def ref = selection
  lazy val selection = system actorSelection path

  def !(msg: Any)(implicit sender: ActorRef = Actor.noSender) { selection ! msg }

  def ?(msg: Any)(implicit timeout: Timeout, sender: ActorRef = Actor.noSender): Fu[Any] = selection ? msg
}

object ActorLazyRef {

  def apply(system: ActorSystem)(path: String): ActorLazyRef = 
    new ActorLazyRef(system, "/user/" + path)
}
