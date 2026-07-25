package lila.core
package clas

import scalalib.data.LazyFu

import lila.core.userId.UserId
import lila.core.id.ClasId
import lila.core.user.Me

enum ClasBus:
  case CanKidsUseMessages(kid1: UserId, kid2: UserId, promise: Promise[Boolean])
  case IsTeacherOf(teacher: UserId, student: UserId, promise: Promise[Boolean])
  case ClasMatesAndTeachers(kid: UserId, promise: Promise[Set[UserId]])

case class ClasTeamConfig(name: String, teacherIds: NonEmptyList[UserId], studentIds: LazyFu[List[UserId]])
case class ClasTeamUpdate(clasId: ClasId, wantsTeam: Option[ClasTeamConfig])(using val teacher: Option[Me])

opaque type MyTeacherIds = Set[UserId]
object MyTeacherIds extends TotalWrapper[MyTeacherIds, Set[UserId]]

opaque type MyStudentIds = Set[UserId]
object MyStudentIds extends TotalWrapper[MyStudentIds, Set[UserId]]
