package lila.clas

import scalalib.SecureRandom
import reactivemongo.api.bson.Macros.Annotations.Key

import lila.core.perf.UserPerfs
import lila.core.perf.UserWithPerfs

case class Student(
    @Key("_id") id: Student.Id, // userId:clasId
    userId: UserId,
    clasId: Clas.Id,
    realName: String,
    notes: String,
    managed: Boolean, // created for the class by the teacher
    created: Clas.Recorded,
    archived: Option[Clas.Recorded]
):
  def is(other: Student) = id == other.id

  def isArchived = archived.isDefined
  def isActive   = !isArchived

object Student:

  given UserIdOf[Student] = _.userId

  opaque type Id = String
  object Id extends OpaqueString[Id]

  def makeId(userId: UserId, clasId: Clas.Id) = Id(s"$userId:$clasId")

  def make(user: User, clas: Clas, teacherId: UserId, realName: String, managed: Boolean) =
    Student(
      id = makeId(user.id, clas.id),
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
    def withPerfs = UserWithPerfs(user, perfs)

  case class WithUserAndManagingClas(withUser: WithUserPerfs, managingClas: Option[Clas]):
    export withUser.*

  case class WithPassword(student: Student, password: lila.user.ClearPassword)

  case class ManagedInfo(createdBy: User, clas: Clas)

  private[clas] object password:

    private val chars      = ('2' to '9') ++ (('a' to 'z').toSet - 'l') mkString
    private val nbChars    = chars.length
    private def secureChar = chars(SecureRandom.nextInt(nbChars))

    def generate = lila.user.ClearPassword:
      String(Array.fill(7)(secureChar))
