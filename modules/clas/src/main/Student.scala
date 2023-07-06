package lila.clas

import ornicar.scalalib.SecureRandom

import lila.user.{ User, UserPerfs }
import lila.rating.Perf

case class Student(
    _id: Student.Id, // userId:clasId
    userId: UserId,
    clasId: Clas.Id,
    realName: String,
    notes: String,
    managed: Boolean, // created for the class by the teacher
    created: Clas.Recorded,
    archived: Option[Clas.Recorded]
):

  inline def id = _id

  def is(user: User)     = userId == user.id
  def is(other: Student) = id == other.id

  def isArchived = archived.isDefined
  def isActive   = !isArchived

object Student:

  opaque type Id = String
  object Id extends OpaqueString[Id]

  def id(userId: UserId, clasId: Clas.Id) = Id(s"$userId:$clasId")

  def make(user: User, clas: Clas, teacherId: UserId, realName: String, managed: Boolean) =
    Student(
      _id = id(user.id, clas.id),
      userId = user.id,
      clasId = clas.id,
      realName = realName,
      notes = "",
      managed = managed,
      created = Clas.Recorded(teacherId, nowInstant),
      archived = none
    )

  trait WithUserLike:
    val student: Student
    val user: User
  case class WithUser(student: Student, user: User) extends WithUserLike:
    def withPerfs(perfs: UserPerfs) = WithUserPerfs(student, user, perfs)
  case class WithUserPerf(student: Student, user: User, perf: Perf) extends WithUserLike
  case class WithUserPerfs(student: Student, user: User, perfs: UserPerfs) extends WithUserLike:
    def withPerfs = User.WithPerfs(user, perfs)

  case class WithUserAndManagingClas(withUser: WithUserPerfs, managingClas: Option[Clas]):
    export withUser.*

  case class WithPassword(student: Student, password: User.ClearPassword)

  case class ManagedInfo(createdBy: User, clas: Clas)

  private[clas] object password:

    private val chars      = ('2' to '9') ++ (('a' to 'z').toSet - 'l') mkString
    private val nbChars    = chars.length
    private def secureChar = chars(SecureRandom nextInt nbChars)

    def generate = User.ClearPassword:
      String(Array.fill(7)(secureChar))
