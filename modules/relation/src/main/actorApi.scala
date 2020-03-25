package lila.relation
package actorApi

import lila.common.LightUser

case class OnlineFriends(users: List[LightUser], playing: Set[String], studying: Set[String]) {
  def isEmpty = users.isEmpty
  def patrons: List[String] = users collect {
    case u if u.isPatron => u.id
  }
}
object OnlineFriends {
  val empty = OnlineFriends(Nil, Set.empty, Set.empty)
}
