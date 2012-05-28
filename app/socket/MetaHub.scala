package lila
package socket

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import play.api.libs.concurrent._
import play.api.Play.current

final class MetaHub(hubs: List[ActorRef]) {

  implicit val executor = Akka.system.dispatcher
  implicit val timeout = Timeout(200 millis)

  def !(message: Any) {
    hubs foreach (_ ! message)
  }

  def ?[A](message: Any)(implicit m: Manifest[A]): Future[List[A]] =
    Future.traverse(hubs) { hub ⇒
      hub ? message mapTo m
    }
}
