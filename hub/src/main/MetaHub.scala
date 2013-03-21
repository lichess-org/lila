package lila.hub

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import actorApi._

final class MetaHub(hubs: List[ActorRef], timeout: FiniteDuration) {

  private implicit val akkaTimeout = Timeout(timeout)

  def to[A : Writes](userId: String, typ: String, data: A) {
    this ! SendTo(userId, Json.obj("t" -> typ, "d" -> data))
  }

  def !(message: Any) {
    hubs foreach (_ ! message)
  }

  def ?[A](message: Any)(implicit m: Manifest[A]): Fu[List[A]] =
    Future.traverse(hubs) { hub ⇒ hub ? message mapTo m }
}
