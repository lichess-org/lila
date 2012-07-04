package lila
package notification

import user.User

import collection.mutable

final class Api {

  private val repo = mutable.Map[String, List[Notification]]()

  def add(user: User, html: String, from: Option[User] = None) {
    repo.update(user.id, Notification(user, html, from) :: get(user))
  }

  def get(user: User): List[Notification] = ~(repo get user.id) 

  def remove(user: User, id: String) {
    repo.update(user.id, get(user) filter (_.id != id))
  }
}
