package lidraughts.socket

import scala.collection.mutable.AnyRefMap

/*
 * NOT thread safe
 * Use in an actor
 */
final class MemberGroup[M <: SocketMember](groupOf: M => Option[String]) {

  private type Group = String
  private type UID = String

  private val groups = AnyRefMap.empty[Group, AnyRefMap[UID, M]]

  def add(uid: UID, member: M): Unit = groupOf(member) foreach { group =>
    groups get group match {
      case None => groups += (group -> AnyRefMap(uid -> member))
      case Some(members) => members += (uid -> member)
    }
  }

  def remove(uid: UID, member: M): Unit = groupOf(member) foreach { group =>
    groups get group foreach { members =>
      members -= uid
      if (members.isEmpty) groups -= group
    }
  }

  def get(group: Group) = groups get group

  def keys = groups.keys
}
