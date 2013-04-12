package lila.hub

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import actorApi._

final class Broadcast(routees: List[ActorRef])(implicit timeout: Timeout) extends Actor {

  def receive = {

    case Ask(msg) => askAll(msg) pipeTo sender

    case msg ⇒ tellAll(msg.pp)
  }

  // def to[A : Writes](userId: String, typ: String, data: A) {
  //   this ! SendTo(userId, Json.obj("t" -> typ, "d" -> data))
  // }

  private def tellAll(message: Any) {
    routees foreach (_ ! message)
  }

  private def askAll(message: Any): Fu[List[Any]] =
    routees.map(_ ? message).sequence
}
