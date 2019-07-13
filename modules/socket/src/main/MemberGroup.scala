package lila.socket

import scala.collection.mutable.AnyRefMap

/*
 * NOT thread safe
 * Use in an actor
 */
final class MemberGroup[M <: SocketMember](groupOf: M => Option[String]) {

  private type Group = String
  private type SriString = String

  private val groups = AnyRefMap.empty[Group, AnyRefMap[SriString, M]]

  def add(sri: Socket.Sri, member: M): Unit = groupOf(member) foreach { group =>
    groups get group match {
      case None => groups += (group -> AnyRefMap(sri.value -> member))
      case Some(members) => members += (sri.value -> member)
    }
  }

  def remove(sri: Socket.Sri, member: M): Unit = groupOf(member) foreach { group =>
    groups get group foreach { members =>
      members -= sri.value
      if (members.isEmpty) groups -= group
    }
  }

  def get(group: Group) = groups get group

  def keys = groups.keys
}
