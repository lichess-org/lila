package lila.relation
package actorApi

import lila.common.LightUser

private[relation] case object ReloadAllOnlineFriends
private[relation] case class AllOnlineFriends(onlines: Map[ID, LightUser])

private[relation] case object NotifyMovement
private[relation] case class Movement(leaves: List[LightUser], enters: List[LightUser])
