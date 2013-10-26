package lila.notification

import scala.collection.mutable

import akka.actor.ActorSelection
import akka.event.EventStream
import akka.pattern.{ ask, pipe }
import play.api.templates.Html

import lila.hub.actorApi.SendTo
import lila.user.User

private[notification] final class Api(bus: EventStream, renderer: ActorSelection) {

  private val repo = mutable.Map[String, List[Notification]]()
  import makeTimeout.large

  def add(userId: String, html: String, from: Option[String] = None) {
    val notif = Notification(userId, html, from)
    repo.update(userId, notif :: get(userId))
    val request = actorApi.RenderNotification(notif.id, notif.from, notif.html)
    renderer ? request foreach {
      case rendered: Html ⇒ bus publish {
        SendTo(userId, "notificationAdd", rendered.toString)
      }
    } 
  }

  def get(userId: String): List[Notification] = ~(repo get userId)

  def remove(userId: String, id: String) {
    repo.update(userId, get(userId) filter (_.id != id))
    bus publish SendTo(userId, "notificationRemove", id)
  }
}
