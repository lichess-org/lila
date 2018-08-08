package lila.socket

import scala.collection.mutable.AnyRefMap

/*
 * NOT thread safe
 * Use in an actor
 */
final class MemberGroup[M <: SocketMember](groupOf: M => Option[String]) {

  private type Group = String
  private type UidString = String

  private val groups = AnyRefMap.empty[Group, AnyRefMap[UidString, M]]

  def add(uid: Socket.Uid, member: M): Unit = groupOf(member) foreach { group =>
    groups get group match {
      case None => groups += (group -> AnyRefMap(uid.value -> member))
      case Some(members) => members += (uid.value -> member)
    }
  }

  def remove(uid: Socket.Uid, member: M): Unit = groupOf(member) foreach { group =>
    groups get group foreach { members =>
      members -= uid.value
      if (members.isEmpty) groups -= group
    }
  }

  def get(group: Group) = groups get group

  def keys = groups.keys
}
