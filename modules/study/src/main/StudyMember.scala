package lila.study

case class StudyMember(id: UserId, role: StudyMember.Role):

  def canContribute = role.canWrite

object StudyMember:

  given UserIdOf[StudyMember] = _.id

  type MemberMap = Map[UserId, StudyMember]

  def make(user: User) = StudyMember(id = user.id, role = Role.Read)

  enum Role(val id: String, val canWrite: Boolean):
    case Read  extends Role("r", false)
    case Write extends Role("w", true)
  object Role:
    val byId = values.mapBy(_.id)

case class StudyMembers(members: StudyMember.MemberMap):

  def +(member: StudyMember) = copy(members = members + (member.id -> member))
  def -(userId: UserId)      = copy(members = members - userId)

  def update(id: UserId, f: StudyMember => StudyMember) = copy(
    members = members.view.mapValues { m =>
      if m.id == id then f(m) else m
    }.toMap
  )

  def contains[U: UserIdOf](u: U): Boolean = members.contains(u.id)

  export members.{ get, keys as ids, keySet as idSet }

  def contributorIds: Set[UserId] =
    members.view.collect {
      case (id, member) if member.canContribute => id
    }.toSet

object StudyMembers:
  val empty = StudyMembers(Map.empty)

  case class OnChange(study: Study)
  object OnChange extends scalalib.bus.GivenChannel[OnChange]("study.members")
