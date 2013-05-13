package lila.hub

import scala.concurrent.duration._
import akka.actor._
import akka.routing._
import akka.dispatch.Dispatchers
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import actorApi._

final class Broadcast(lazyRefs: List[ActorLazyRef])(implicit timeout: Timeout) extends Actor {

  def receive = {

    case GetNbMembers ⇒ askAll(GetNbMembers).mapTo[List[Int]] foreach { nbs ⇒
      broadcast(NbMembers(nbs.sum))
    }

    case Ask(msg) ⇒ askAll(msg) pipeTo sender

    case msg      ⇒ broadcast(msg)
  }

  private def broadcast(msg: Any) {
    actors foreach { _ ! msg }
  }

  private def actors = lazyRefs map (_.ref)

  private def askAll(message: Any): Fu[List[Any]] =
    actors.map(_ ? message).sequence
}
