package lila
package notification

import user.User
import socket.MetaHub

import collection.mutable

final class Api(metaHub: MetaHub) {

  private val repo = mutable.Map[String, List[Notification]]()

  def add(user: User, html: String, from: Option[User] = None) {
    val notification = Notification(user, html, from)
    repo.update(user.id, notification :: get(user))
    metaHub.addNotification(user.id, views.html.notification.view(notification).toString)
  }

  def get(user: User): List[Notification] = ~(repo get user.id) 

  def remove(user: User, id: String) {
    repo.update(user.id, get(user) filter (_.id != id))
    metaHub.removeNotification(user.id, id)
  }
}
