package lila.relation
package actorApi

import lila.common.LightUser

private[relation] case class AllOnlineFriends(onlines: Map[ID, LightUser])
private[relation] case object NotifyMovement
