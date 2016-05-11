package lila.study

import org.joda.time.DateTime

import lila.common.LightUser
import lila.user.User

case class StudyMember(
    id: User.ID,
    role: StudyMember.Role,
    addedAt: DateTime) {

  def canContribute = role == StudyMember.Role.Write
}

object StudyMember {

  type MemberMap = Map[User.ID, StudyMember]

  def make(user: User) = StudyMember(id = user.id, role = Role.Read, addedAt = DateTime.now)

  sealed abstract class Role(val id: String)
  object Role {
    case object Read extends Role("r")
    case object Write extends Role("w")
    val byId = List(Read, Write).map { x => x.id -> x }.toMap
  }
}

case class StudyMembers(members: StudyMember.MemberMap) {

  def +(member: StudyMember) = copy(members = members + (member.id -> member))

  def contains(userId: User.ID): Boolean = members contains userId
  def contains(user: User): Boolean = contains(user.id)

  def get = members.get _

  def ids = members.keys
}
