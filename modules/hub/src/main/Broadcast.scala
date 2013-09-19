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

    case GetNbMembers ⇒ askAll(GetNbMembers).mapTo[List[Int]] foreach { nbs ⇒
      broadcast(NbMembers(nbs.sum))
    }

    case Ask(msg) ⇒
      askAll(msg) logFailure ("[broadcast] " + Ask(msg)) pipeTo sender

    case msg ⇒ broadcast(msg)
  }

  private def broadcast(msg: Any) {
    actors foreach { _ ! msg }
  }

  private def askAll(message: Any): Fu[List[Any]] =
    actors.map(_ ? message).sequenceFu
}
