package lila.study

import lila.user.User

case class StudyMember(id: User.ID, role: StudyMember.Role) {

  def canContribute = role.canWrite
}

object StudyMember {

  type MemberMap = Map[User.ID, StudyMember]

  def make(user: User, role: StudyMember.Role = Role.Read) =
    StudyMember(id = user.id, role = role)

  sealed abstract class Role(val id: String, val canWrite: Boolean)
  object Role {
    case object Read  extends Role("r", false)
    case object Write extends Role("w", true)
    val byId = List(Read, Write).map { x =>
      x.id -> x
    }.toMap
  }
}

case class StudyMembers(members: StudyMember.MemberMap) {

  def +(member: StudyMember) = copy(members = members + (member.id -> member))
  def -(userId: User.ID)     = copy(members = members - userId)

  def update(id: User.ID, f: StudyMember => StudyMember) = copy(
    members = members.view.mapValues { m =>
      if (m.id == id) f(m) else m
    }.toMap
  )

  def contains(userId: User.ID): Boolean = members contains userId
  def contains(user: User): Boolean      = contains(user.id)

  def get = members.get _

  def ids   = members.keys
  def idSet = members.keySet

  def contributorIds: Set[User.ID] =
    members.view.collect {
      case (id, member) if member.canContribute => id
    }.toSet
}

object StudyMembers {
  val empty = StudyMembers(Map.empty)
}
