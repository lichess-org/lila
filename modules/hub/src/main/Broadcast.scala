package lila.hub

import scala.concurrent.duration._

import akka.actor._
import akka.dispatch.Dispatchers
import akka.pattern.{ ask, pipe }
import akka.routing._
import akka.util.Timeout

import actorApi._

final class Broadcast(actors: List[ActorSelection])(implicit timeout: Timeout) extends Actor {

  def receive = {

    case GetNbMembers ⇒
      actors.map(_ ? GetNbMembers mapTo manifest[Int]).suml foreach { nb ⇒
        broadcast(NbMembers(nb))
      }

    case msg ⇒ broadcast(msg.pp)
  }

  private def broadcast(msg: Any) {
    actors foreach { _ ! msg }
  }
}
