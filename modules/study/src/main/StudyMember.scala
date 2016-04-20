package lila.study

import lila.common.LightUser

case class StudyMember(
    user: LightUser,
    position: Position.Ref,
    role: StudyMember.Role) {

  def canWrite = role == StudyMember.Role.Write
}

object StudyMember {

  sealed abstract class Role(val id: String)
  object Role {
    case object Read extends Role("r")
    case object Write extends Role("w")
    val byId = List(Read, Write).map { x => x.id -> x }.toMap
  }
}
