package lila.study

import lila.user.User

case class StudyMember(id: UserId, role: StudyMember.Role):

  def canContribute = role.canWrite

object StudyMember:

  type MemberMap = Map[UserId, StudyMember]

  def make(user: User) = StudyMember(id = user.id, role = Role.Read)

  sealed abstract class Role(val id: String, val canWrite: Boolean)
  object Role:
    case object Read  extends Role("r", false)
    case object Write extends Role("w", true)
    val byId = List(Read, Write).map { x =>
      x.id -> x
    }.toMap

case class StudyMembers(members: StudyMember.MemberMap):

  def +(member: StudyMember) = copy(members = members + (member.id -> member))
  def -(userId: UserId)      = copy(members = members - userId)

  def update(id: UserId, f: StudyMember => StudyMember) = copy(
    members = members.view.mapValues { m =>
      if (m.id == id) f(m) else m
    }.toMap
  )

  def contains(userId: UserId): Boolean = members contains userId
  def contains(user: User): Boolean     = contains(user.id)

  def get = members.get

  def ids   = members.keys
  def idSet = members.keySet

  def contributorIds: Set[UserId] =
    members.view.collect {
      case (id, member) if member.canContribute => id
    }.toSet

object StudyMembers:
  val empty = StudyMembers(Map.empty)
