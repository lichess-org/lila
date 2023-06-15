package lila.clas

import ornicar.scalalib.SecureRandom

import lila.user.User

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

  case class WithUser(student: Student, user: User)

  case class WithUserAndManagingClas(withUser: WithUser, managingClas: Option[Clas]):
    def student = withUser.student
    def user    = withUser.user

  case class WithPassword(student: Student, password: User.ClearPassword)

  case class ManagedInfo(createdBy: User, clas: Clas)

  private[clas] object password:

    private val chars      = ('2' to '9') ++ (('a' to 'z').toSet - 'l') mkString
    private val nbChars    = chars.length
    private def secureChar = chars(SecureRandom nextInt nbChars)

    def generate =
      User.ClearPassword {
        new String(Array.fill(7)(secureChar))
      }
