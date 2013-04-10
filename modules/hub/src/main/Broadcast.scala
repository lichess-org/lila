package lila.hub

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import actorApi._

final class Broadcast(routees: List[ActorRef])(implicit timeout: Timeout) extends Actor {

  def receive = {

    case msg: SendTo ⇒ tell(msg)

    case msg ⇒ tell(msg)
  }

  // def to[A : Writes](userId: String, typ: String, data: A) {
  //   this ! SendTo(userId, Json.obj("t" -> typ, "d" -> data))
  // }

  private def tell(message: Any) {
    routees foreach (_ ! message)
  }

  // private def ask[A](message: Any)(implicit m: Manifest[A]): Fu[List[A]] =
  //   Future.traverse(routees) { hub ⇒ hub ? message mapTo m }
}
