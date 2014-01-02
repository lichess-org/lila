package lila.relation
package actorApi

private[relation] case object ReloadAllOnlineFriends
private[relation] case class AllOnlineFriends(onlines: Map[ID, Username])

private[relation] case object NotifyMovement
private[relation] case class Movement(leaves: List[User], enters: List[User])
