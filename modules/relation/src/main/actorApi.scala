package lila.relation
package actorApi

import lila.common.LightUser

private[relation] case class AllOnlineFriends(onlines: Map[ID, LightUser])
private[relation] case object ComputeMovement

case class OnlineFriends(users: List[LightUser], playing: Set[String], studying: Set[String]) {
  def patrons: List[String] = users collect {
    case u if u.isPatron => u.id
  }
}
object OnlineFriends {
  val empty = OnlineFriends(Nil, Set.empty, Set.empty)
}
