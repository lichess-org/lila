package lila.hub

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.{ ask, pipe }
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import actorApi._

final class Broadcast(
    routees: List[ActorRef],
    timeout: FiniteDuration) extends Actor {

  private implicit val akkaTimeout = makeTimeout(timeout)

  def receive = {

    case msg: SendTo[_] ⇒ tell(msg)

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
