package lila
package socket

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import scala.concurrent._
import scala.concurrent.duration._
import akka.util.{ Duration, Timeout }
import play.api.libs.concurrent._
import play.api.Play.current
import play.api.libs.json._

final class MetaHub(hubs: List[ActorRef]) {

  implicit val executor = Akka.system.dispatcher
  implicit val timeout = Timeout(1 second)

  def !(message: Any) {
    hubs foreach (_ ! message)
  }

  def ?[A](message: Any)(implicit m: Manifest[A]): Future[List[A]] =
    Future.traverse(hubs) { hub ⇒
      hub ? message mapTo m
    }
  
  def notifyUnread(userId: String, nb: Int) = 
    notify(userId, "nbm", JsNumber(nb))

  def addNotification(userId: String, html: String) = 
    notify(userId, "notificationAdd", JsString(html))

  def removeNotification(userId: String, id: String) = 
    notify(userId, "notificationRemove", JsString(id))

  private def notify(userId: String, typ: String, data: JsValue) =
    this ! SendTo(userId, JsObject(Seq("t" -> JsString(typ), "d" -> data)))

}
