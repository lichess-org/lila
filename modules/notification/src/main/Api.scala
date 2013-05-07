package lila.notification

import lila.user.User
import lila.hub.actorApi.SendTo

import scala.collection.mutable
import play.api.templates.Html
import akka.actor.ActorRef
import akka.pattern.ask

private[notification] final class Api(socketHub: ActorRef, renderer: ActorRef) {

  private val repo = mutable.Map[String, List[Notification]]()
  import makeTimeout.large

  def add(userId: String, html: String, from: Option[String] = None) {
    val notif = Notification(userId, html, from)
    repo.update(userId, notif :: get(userId))
    val request = actorApi.RenderNotification(notif.id, notif.from, notif.html)
    renderer ? request mapTo manifest[Html] onSuccess {
      case rendered ⇒ socketHub ! SendTo(userId, "notificationAdd", rendered.toString)
    }
  }

  def get(userId: String): List[Notification] = ~(repo get userId)

  def remove(userId: String, id: String) {
    repo.update(userId, get(userId) filter (_.id != id))
    socketHub ! SendTo(userId, "notificationRemove", id)
  }
}
