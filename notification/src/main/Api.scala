package lila.notification

import lila.user.User

import scala.collection.mutable
import play.api.templates.Html
import play.api.libs.concurrent.Execution.Implicits._
import akka.actor.ActorRef
import akka.pattern.ask

final class Api(metaHub: ActorRef, renderer: ActorRef) {

  private val repo = mutable.Map[String, List[Notification]]()
  private implicit val timeout = makeTimeout.large

  def add(userId: String, html: String, from: Option[String] = None) {
    val notif = Notification(userId, html, from)
    repo.update(userId, notif :: get(userId))
    val request = actorApi.RenderNotification(notif.id, notif.from, notif.html) 
    renderer ? request mapTo manifest[Html] onSuccess { 
      case rendered => metaHub ! 
        .addNotification(userId, rendered.toString)
    }
  }

  def get(userId: String): List[Notification] = ~(repo get userId) 

  def remove(userId: String, id: String) {
    repo.update(userId, get(userId) filter (_.id != id))
    metaHub.removeNotification(userId, id)
  }
}
